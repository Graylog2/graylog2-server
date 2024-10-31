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
package org.graylog2.outputs;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Size;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.messages.ImmutableMessage;
import org.graylog2.indexer.messages.SerializationContext;
import org.graylog2.outputs.filter.FilteredMessage;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * A thread-safe and index set aware output buffer implementation.
 * <p>
 * This buffer class is aware of index sets and calculates the remaining buffer capacity based on the number of
 * index sets per message.
 * If a message has two different index sets, the indexer output will create one message entry per index
 * set in the bulk request against OpenSearch.
 * <p>
 * To avoid bulk requests that get too big, we reserve one buffer slot per message and index set.
 * The trade-off is that outputs which don't create one message per index set will write smaller batches.
 */
public class IndexSetAwareMessageOutputBuffer {
    private final int maxBufferSizeCount;
    private final long maxBufferSizeBytes;
    private final ObjectMapper objectMapper;

    private volatile List<FilteredMessage> buffer;
    private volatile int bufferLength = 0;
    private volatile long bufferSizeBytes = 0L;
    private final AtomicLong lastFlushTime = new AtomicLong();

    /**
     * Creates a new buffer with the given size.
     *
     * @param maxBufferSize the maximum buffer size
     */
    @Inject
    public IndexSetAwareMessageOutputBuffer(@Named("output_batch_size") BatchSizeConfig maxBufferSize,
                                            ObjectMapper objectMapper) {

        this.maxBufferSizeCount = maxBufferSize.getAsCount().orElse(0);
        this.maxBufferSizeBytes = maxBufferSize.getAsBytes().map(Size::toBytes).orElse(0L);
        this.buffer = new ArrayList<>(maxBufferSize.getAsCount().orElse(500));

        this.objectMapper = objectMapper;
    }

    /**
     * Checks if the time of the last buffer flush is larger than the given flush interval.
     * <p>
     * This method is thread-safe.
     *
     * @param flushInterval the flush interval duration
     * @return true if the time of the last buffer flush is larger than the given flush interval. Otherwise, false.
     */
    public boolean shouldFlush(Duration flushInterval) {
        final long lastFlush = lastFlushTime.get();
        // If we don't know the last flush time, we want to flush. Happens with a new buffer instance.
        return lastFlush == 0 || (System.nanoTime() - lastFlush) > flushInterval.toNanos();
    }

    /**
     * Appends the given message to the buffer. If the buffer length has reached the configured max buffer size,
     * the given flush consumer is called with the contents of the buffer and the buffer is reset.
     * The consumer is responsible for handling the buffer content.
     * <p>
     * This method is thread-safe.
     *
     * @param filteredMessage the message to append to the buffer
     * @param flusher         the buffer flush consumer
     */
    public void appendAndFlush(FilteredMessage filteredMessage, Consumer<List<FilteredMessage>> flusher) {
        List<FilteredMessage> flushBatch = null;
        synchronized (this) {
            // See class the class documentation for the reasoning behind the bufferLength calculation.
            buffer.add(filteredMessage);
            bufferLength += Math.max(filteredMessage.message().getIndexSets().size(), 1);
            // for optimization, only calculate batch size in bytes, if we are actually restricting by size in bytes
            if (maxBufferSizeBytes != 0L) {
                bufferSizeBytes += estimateOsBulkRequestSize(filteredMessage.message(), objectMapper);
            }

            if ((maxBufferSizeBytes != 0L && bufferSizeBytes >= maxBufferSizeBytes) ||
                    maxBufferSizeCount != 0 && bufferLength >= maxBufferSizeCount) {
                flushBatch = buffer;
                buffer = new ArrayList<>(bufferLength);
                bufferLength = 0;
                bufferSizeBytes = 0L;
            }
        }
        // if the current thread found it had to flush any messages, it does so but blocks.
        // this ensures we don't flush more than 'processorCount' in parallel.
        // TODO this will still be time limited by the OutputBufferProcessor and thus be called more often than it should
        if (flushBatch != null) {
            lastFlushTime.set(System.nanoTime());
            flusher.accept(flushBatch);
        }
    }

    /**
     * Calls the given flush consumer with the contents of the buffer and the buffer is reset. The consumer is
     * responsible for handling the buffer content.
     * <p>
     * This method is thread-safe.
     *
     * @param flusher the buffer flush consumer
     */
    public void flush(Consumer<List<FilteredMessage>> flusher) {
        final List<FilteredMessage> flushBatch;
        synchronized (this) {
            flushBatch = buffer;
            buffer = new ArrayList<>(bufferLength);
            bufferLength = 0;
            bufferSizeBytes = 0L;
        }
        if (flushBatch != null) {
            lastFlushTime.set(System.nanoTime());
            flusher.accept(flushBatch);
        }
    }

    /**
     * Get a ballpark figure for the size in bytes that the OpenSarch bulk request for a message will require.
     */
    @VisibleForTesting
    static long estimateOsBulkRequestSize(ImmutableMessage message, ObjectMapper objectMapper) {
        // Get size of the message by preemptively serializing it. The implementation of ImmutableMessage is expected
        // to cache the result so that serialization won't be performed twice.
        long msgSize;
        try {
            msgSize = message.serialize(SerializationContext.of(objectMapper, new Meter())).length + 1; // msg size plus newline
        } catch (IOException e) {
            msgSize = 0;
        }

        // An OpenSearch bulk index request will include an "index" instruction for each message and each index set.
        // An instruction will look like this:
        // {"index":{"_index":"graylog_deflector","_id":"70db7111-48fd-11ef-b7e8-5ae4251f926d"}}
        // Take that into account as well.
        final long indexInstructionsSize = message.getIndexSets().stream()
                .map(IndexSet::getWriteIndexAlias)
                .mapToLong(index -> 32L + MoreObjects.firstNonNull(index, "").length() + 36L + 1) // instruction size plus newline
                .sum();

        return indexInstructionsSize + msgSize * Math.max(message.getIndexSets().size(), 1);
    }
}
