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
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.shared.buffers.RawMessageEvent;
import org.graylog2.shared.journal.Journal;
import org.graylog2.shared.journal.LocalKafkaJournal;
import org.graylog2.shared.messageq.MessageQueueException;
import org.graylog2.shared.messageq.MessageQueueWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Singleton
public class LocalKafkaMessageQueueWriter extends AbstractIdleService implements MessageQueueWriter {
    private static final Logger LOG = LoggerFactory.getLogger(LocalKafkaMessageQueueWriter.class);

    private LocalKafkaJournal kafkaJournal;
    private Semaphore journalFilled;
    private final Metrics metrics;
    private final Retryer<Void> writeRetryer;

    @Inject
    public LocalKafkaMessageQueueWriter(LocalKafkaJournal kafkaJournal,
                                        @Named("JournalSignal") Semaphore journalFilled,
                                        MessageQueueWriter.Metrics metrics) {
        this.kafkaJournal = kafkaJournal;
        this.journalFilled = journalFilled;
        this.metrics = metrics;

        writeRetryer = RetryerBuilder.<Void>newBuilder()
                .retryIfException(input -> {
                    LOG.error("Unable to write to journal - retrying with exponential back-off", input);
                    metrics.failedWriteAttempts().mark();
                    return true;
                })
                .withWaitStrategy(WaitStrategies.exponentialWait(250, 1, TimeUnit.MINUTES))
                .withStopStrategy(StopStrategies.neverStop())
                .build();
    }

    @Override
    public void write(List<RawMessageEvent> entries) throws MessageQueueException {

        final AtomicLong msgBytes = new AtomicLong(0);

        final List<Journal.Entry> journalEntries = entries.stream()
                .filter(Objects::nonNull)
                .map(e -> new Journal.Entry(e.getMessageIdBytes(), e.getEncodedRawMessage()))
                .peek(e -> msgBytes.addAndGet(e.getMessageBytes().length))
                .collect(Collectors.toList());

        try {
            writeToJournal(journalEntries);
        } catch (Exception e) {
            LOG.error("Unable to write to journal - retrying", e);

            // Use retryer with exponential back-off to avoid spamming the logs.
            try {
                writeRetryer.call(() -> {
                    writeToJournal(journalEntries);
                    return null;
                });
            } catch (ExecutionException | RetryException ex) {
                throw new MessageQueueException("Retryer exception", ex);
            }
        }

        metrics.writtenMessages().mark(journalEntries.size());
        metrics.writtenBytes().mark(msgBytes.get());
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
}
