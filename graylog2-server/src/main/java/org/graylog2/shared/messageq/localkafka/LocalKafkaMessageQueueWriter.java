/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.messageq.localkafka;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.shared.buffers.RawMessageEvent;
import org.graylog2.shared.journal.Journal;
import org.graylog2.shared.journal.LocalKafkaJournal;
import org.graylog2.shared.messageq.MessageQueueException;
import org.graylog2.shared.messageq.MessageQueueWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.transform;

@Singleton
public class LocalKafkaMessageQueueWriter extends AbstractIdleService implements MessageQueueWriter {
    private static final Logger LOG = LoggerFactory.getLogger(LocalKafkaMessageQueueWriter.class);

    private LocalKafkaJournal kafkaJournal;
    private Semaphore journalFilled;

    private static final Retryer<Void> JOURNAL_WRITE_RETRYER = RetryerBuilder.<Void>newBuilder()
            .retryIfException(new Predicate<Throwable>() {
                @Override
                public boolean apply(@Nullable Throwable input) {
                    LOG.error("Unable to write to journal - retrying with exponential back-off", input);
                    return true;
                }
            })
            .withWaitStrategy(WaitStrategies.exponentialWait(250, 1, TimeUnit.MINUTES))
            .withStopStrategy(StopStrategies.neverStop())
            .build();


    @Inject
    public LocalKafkaMessageQueueWriter(LocalKafkaJournal kafkaJournal,
                                        @Named("JournalSignal") Semaphore journalFilled) {
        this.kafkaJournal = kafkaJournal;
        this.journalFilled = journalFilled;
    }

    @Override
    public void write(List<RawMessageEvent> entries) throws MessageQueueException {
        final Converter converter = new Converter();
        final ArrayList<Journal.Entry> journalEntries = Lists.newArrayList(transform(entries, converter));

        try {
            writeToJournal(journalEntries);
        } catch (Exception e) {
            LOG.error("Unable to write to journal - retrying", e);

            // Use retryer with exponential back-off to avoid spamming the logs.
            try {
                JOURNAL_WRITE_RETRYER.call(() -> {
                    writeToJournal(journalEntries);
                    return null;
                });
            } catch (ExecutionException | RetryException ex) {
                throw new MessageQueueException("Retryer exception", ex);
            }
        }
    }

    private void writeToJournal(List<Journal.Entry> entries) {
        final long lastOffset = kafkaJournal.write(entries);
        LOG.debug("Processed batch, last journal offset: {}, signalling reader.",
                lastOffset);
        journalFilled.release();
    }

    @Override
    protected void startUp() throws Exception {

    }

    @Override
    protected void shutDown() throws Exception {

    }

    private static class Converter implements Function<RawMessageEvent, Journal.Entry> {
        @Nullable
        @Override
        public Journal.Entry apply(@Nullable RawMessageEvent input) {
            return new Journal.Entry(input.getMessageIdBytes(), input.getEncodedRawMessage());
        }
    }
}
