package org.graylog2.outputs;

import org.graylog2.outputs.filter.FilteredMessage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class IndexSetAwareMessageOutputBuffer {
    private final int maxBufferSize;
    private volatile List<FilteredMessage> buffer;
    private final AtomicInteger bufferLength = new AtomicInteger();
    private final AtomicLong lastFlushTime = new AtomicLong();

    public IndexSetAwareMessageOutputBuffer(int outputBatchSize) {
        this.maxBufferSize = outputBatchSize;
        this.buffer = new ArrayList<>(maxBufferSize);
    }

    public boolean shouldFlush(Duration flushInterval) {
        final long lastFlush = lastFlushTime.get();
        // If we don't know the last flush time, we want to flush. Happens with a new buffer instance.
        return lastFlush == 0 || (System.nanoTime() - lastFlush) > flushInterval.toNanos();
    }

    public void appendAndFlush(FilteredMessage filteredMessage, Consumer<List<FilteredMessage>> flusher) {
        List<FilteredMessage> flushBatch = null;
        synchronized (this) {
            buffer.add(filteredMessage);
            // TODO: Add description
            bufferLength.addAndGet(Math.max(filteredMessage.message().getIndexSets().size(), 1));

            if (bufferLength.get() >= maxBufferSize) {
                flushBatch = buffer;
                buffer = new ArrayList<>(maxBufferSize);
                bufferLength.set(0);
            }
        }
        // if the current thread found it had to flush any messages, it does so but blocks.
        // this ensures we don't flush more than 'processorCount' in parallel.
        // TODO this will still be time limited by the OutputBufferProcessor and thus be called more often than it should
        if (flushBatch != null) {
            flusher.accept(flushBatch);
            lastFlushTime.set(System.nanoTime());
        }
    }

    public void flush(Consumer<List<FilteredMessage>> flusher) {
        final List<FilteredMessage> flushBatch;
        synchronized (this) {
            flushBatch = buffer;
            buffer = new ArrayList<>(maxBufferSize);
        }
        if (flushBatch != null) {
            flusher.accept(flushBatch);
            lastFlushTime.set(System.nanoTime());
        }
    }
}
