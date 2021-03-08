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
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.lmax.disruptor.EventHandler;
import org.graylog2.shared.messageq.MessageQueueWriter;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class JournallingMessageHandler implements EventHandler<RawMessageEvent> {
    private static final Logger log = LoggerFactory.getLogger(JournallingMessageHandler.class);

    private final List<RawMessageEvent> batch = Lists.newArrayList();
    private final Counter byteCounter;
    private final MessageQueueWriter messageQueueWriter;
    private final ProcessingStatusRecorder processingStatusRecorder;

    @Inject
    public JournallingMessageHandler(MetricRegistry metrics,
                                     MessageQueueWriter messageQueueWriter,
                                     ProcessingStatusRecorder processingStatusRecorder) {
        this.messageQueueWriter = messageQueueWriter;
        this.processingStatusRecorder = processingStatusRecorder;
        byteCounter = metrics.counter(MetricRegistry.name(JournallingMessageHandler.class, "written_bytes"));
    }

    @Override
    public void onEvent(RawMessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        batch.add(event);

        if (endOfBatch) {
            log.debug("End of batch, journaling {} messages", batch.size());
            // write batch to journal

            // copy to avoid re-running this all the time
            final Filter metricsFilter = new Filter();
            final List<RawMessageEvent> entries = batch.stream().map(metricsFilter).filter(Objects::nonNull).collect(Collectors.toList());

            // Clear the batch list after transforming it with the Converter because the fields of the RawMessageEvent
            // objects in there have been set to null and cannot be used anymore.
            batch.clear();

            messageQueueWriter.write(entries);

            // The filter computed the latest receive timestamp of all messages in the batch so we don't have to
            // call the update on the recorder service for every message. (less contention)
            processingStatusRecorder.updateIngestReceiveTime(metricsFilter.getLatestReceiveTime());
        }
    }

    private class Filter implements Function<RawMessageEvent, RawMessageEvent> {
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
        public RawMessageEvent apply(RawMessageEvent input) {
            if (log.isTraceEnabled()) {
                log.trace("Journalling message {}", input.getMessageId());
            }

            if (input.getEncodedRawMessage() == null) {
                log.error("Skipping RawMessageEvent with null encodedRawMessage");
                return null;
            }
            // stats
            final int size = input.getEncodedRawMessage().length;
            bytesWritten += size;
            byteCounter.inc(size);

            final DateTime messageTimestamp = input.getMessageTimestamp();
            if (messageTimestamp != null) {
                latestReceiveTime = latestReceiveTime.isBefore(messageTimestamp) ? messageTimestamp : latestReceiveTime;
            }

            return input;
        }
    }
}
