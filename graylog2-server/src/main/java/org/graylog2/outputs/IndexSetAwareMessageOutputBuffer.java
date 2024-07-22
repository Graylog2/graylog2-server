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

import com.github.joschi.jadconfig.util.Size;
import org.graylog2.outputs.filter.FilteredMessage;

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

    private volatile List<FilteredMessage> buffer;
    private volatile int bufferLength = 0;
    private volatile long bufferSizeBytes = 0L;
    private final AtomicLong lastFlushTime = new AtomicLong();

    /**
     * Creates a new buffer with the given size.
     *
     * @param maxBufferSize the maximum buffer size
     */
    public IndexSetAwareMessageOutputBuffer(BatchSizeConfig maxBufferSize) {
        this.maxBufferSizeCount = maxBufferSize.getAsCount().orElse(0);
        this.maxBufferSizeBytes = maxBufferSize.getAsBytes().map(Size::toBytes).orElse(0L);
        this.buffer = new ArrayList<>(maxBufferSize.getAsCount().orElse(500));
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
            final int requiredSlots = Math.max(filteredMessage.message().getIndexSets().size(), 1);
            bufferLength += requiredSlots;
            bufferSizeBytes += requiredSlots * filteredMessage.message().getSize();

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
        }
        if (flushBatch != null) {
            lastFlushTime.set(System.nanoTime());
            flusher.accept(flushBatch);
        }
    }
}
