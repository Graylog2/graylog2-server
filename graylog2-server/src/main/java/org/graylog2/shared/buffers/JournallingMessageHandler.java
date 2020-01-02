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
package org.graylog2.shared.buffers;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.lmax.disruptor.EventHandler;
import org.graylog2.shared.journal.Journal;
import org.graylog2.shared.messageq.MessageQueue;
import org.graylog2.shared.messageq.MessageQueueWriter;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.transform;

public class JournallingMessageHandler implements EventHandler<RawMessageEvent> {
    private static final Logger log = LoggerFactory.getLogger(JournallingMessageHandler.class);

    private static final Set<Journal.Entry> NULL_SINGLETON = Collections.singleton(null);
    private static final Set<MessageQueue.Entry> MESSAGE_JOURNAL_NULL_SINGLETON = Collections.singleton(null);
    private static final Retryer<Void> JOURNAL_WRITE_RETRYER = RetryerBuilder.<Void>newBuilder()
            .retryIfException(new Predicate<Throwable>() {
                @Override
                public boolean apply(@Nullable Throwable input) {
                    log.error("Unable to write to journal - retrying with exponential back-off", input);
                    return true;
                }
            })
            .withWaitStrategy(WaitStrategies.exponentialWait(250, 1, TimeUnit.MINUTES))
            .withStopStrategy(StopStrategies.neverStop())
            .build();

    private final List<RawMessageEvent> batch = Lists.newArrayList();
    private final Counter byteCounter;
    private final Journal journal;
    private final MessageQueueWriter messageQueueWriter;
    private final ProcessingStatusRecorder processingStatusRecorder;
    private final Semaphore journalFilled;

    @Inject
    public JournallingMessageHandler(MetricRegistry metrics,
                                     Journal journal,
                                     MessageQueueWriter messageQueueWriter,
                                     ProcessingStatusRecorder processingStatusRecorder,
                                     @Named("JournalSignal") Semaphore journalFilled) {
        this.journal = journal;
        this.messageQueueWriter = messageQueueWriter;
        this.processingStatusRecorder = processingStatusRecorder;
        this.journalFilled = journalFilled;
        byteCounter = metrics.counter(MetricRegistry.name(JournallingMessageHandler.class, "written_bytes"));
    }

    @Override
    public void onEvent(RawMessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        batch.add(event);

        if (endOfBatch) {
            log.debug("End of batch, journalling {} messages", batch.size());
            // write batch to journal

            final Converter converter = new Converter();
            final MessageJournalConverter messageJournalConverter = new MessageJournalConverter();
            // Needs to run before the other converter because that one is setting fields to null
            final ArrayList<MessageQueue.Entry> messageQueueEntries = Lists.newArrayList(transform(batch, messageJournalConverter));
            // copy to avoid re-running this all the time
            final List<Journal.Entry> entries = Lists.newArrayList(transform(batch, converter));

            // Remove all null values returned from the converter (might happen if the Converter throws an exception)
            entries.removeAll(NULL_SINGLETON);
            messageQueueEntries.removeAll(MESSAGE_JOURNAL_NULL_SINGLETON);

            // Clear the batch list after transforming it with the Converter because the fields of the RawMessageEvent
            // objects in there have been set to null and cannot be used anymore.
            batch.clear();

            // Catch all exceptions that might happen during the journal write and retry the operation.
            // This basically blocks if the journal write always throws an exception. Once the write succeeds, we will
            // continue.
            try {
                try {
                    messageQueueWriter.write(messageQueueEntries);
                } catch (Exception e) {
                    log.error("Unable to write to MessageJournal", e);
                }
                writeToJournal(converter, entries);

                // The converter computed the latest receive timestamp of all messages in the batch so we don't have to
                // call the update on the recorder service for every message. (less contention)
                processingStatusRecorder.updateIngestReceiveTime(converter.getLatestReceiveTime());
            } catch (Exception e) {
                log.error("Unable to write to journal - retrying", e);

                // Use retryer with exponential back-off to avoid spamming the logs.
                JOURNAL_WRITE_RETRYER.call(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        writeToJournal(converter, entries);
                        return null;
                    }
                });
            }
        }
    }

    private void writeToJournal(Converter converter, List<Journal.Entry> entries) {
        final long lastOffset = journal.write(entries);
        log.debug("Processed batch, wrote {} bytes, last journal offset: {}, signalling reader.",
                converter.getBytesWritten(),
                lastOffset);
        journalFilled.release();
    }

    private class MessageJournalConverter implements Function<RawMessageEvent, MessageQueue.Entry> {
        @Nullable
        @Override
        public MessageQueue.Entry apply(@Nullable RawMessageEvent input) {
            try {
                final byte[] messageIdBytes = input.getMessageIdBytes();
                final byte[] encodedRawMessage = input.getEncodedRawMessage();

                // TODO: Check Converter implementation and copy relevant parts here (e.g. setting fields to null and metrics)

                return messageQueueWriter.createEntry(messageIdBytes, encodedRawMessage);
            } catch (Exception e) {
                log.error("Unable to convert RawMessageEvent to MessageJournal.Entry - skipping event", e);
                return null;
            }
        }
    }

    private class Converter implements Function<RawMessageEvent, Journal.Entry> {
        private long bytesWritten = 0;
        private DateTime latestReceiveTime = new DateTime(0L, DateTimeZone.UTC);

        public long getBytesWritten() {
            return bytesWritten;
        }

        public DateTime getLatestReceiveTime() {
            return latestReceiveTime;
        }

        @Nullable
        @Override
        public Journal.Entry apply(RawMessageEvent input) {
            try {
                if (log.isTraceEnabled()) {
                    log.trace("Journalling message {}", input.getMessageId());
                }
                final byte[] messageIdBytes = input.getMessageIdBytes();
                final byte[] encodedRawMessage = input.getEncodedRawMessage();

                // stats
                final int size = encodedRawMessage.length;
                bytesWritten += size;
                byteCounter.inc(size);

                final DateTime messageTimestamp = input.getMessageTimestamp();
                if (messageTimestamp != null) {
                    latestReceiveTime = latestReceiveTime.isBefore(messageTimestamp) ? messageTimestamp : latestReceiveTime;
                }

                // clear for gc and to avoid promotion to tenured space
                input.setMessageIdBytes(null);
                input.setEncodedRawMessage(null);
                input.setMessageTimestamp(null);
                // convert to journal entry
                return journal.createEntry(messageIdBytes, encodedRawMessage);
            } catch (Exception e) {
                log.error("Unable to convert RawMessageEvent to Journal.Entry - skipping event", e);
                return null;
            }
        }
    }
}
