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

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.Maps;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Message;
import org.graylog2.shared.journal.Journal;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

// Singleton class
public class BlockingBatchedESOutput extends ElasticSearchOutput {
    private static final Logger log = LoggerFactory.getLogger(BlockingBatchedESOutput.class);
    private final int maxBufferSize;
    private final Timer processTime;
    private final Histogram batchSize;
    private final Meter bufferFlushes;
    private final Meter bufferFlushFailures;
    private final Meter bufferFlushesRequested;

    private volatile List<Map.Entry<IndexSet, Message>> buffer;

    private static final AtomicInteger activeFlushThreads = new AtomicInteger(0);
    private final AtomicLong lastFlushTime = new AtomicLong();
    private final int outputFlushInterval;

    @Inject
    public BlockingBatchedESOutput(MetricRegistry metricRegistry,
                                   Messages messages,
                                   org.graylog2.Configuration serverConfiguration,
                                   Journal journal,
                                   MessageQueueAcknowledger acknowledger) {
        super(metricRegistry, messages, journal, acknowledger);
        this.maxBufferSize = serverConfiguration.getOutputBatchSize();
        outputFlushInterval = serverConfiguration.getOutputFlushInterval();
        this.processTime = metricRegistry.timer(name(this.getClass(), "processTime"));
        this.batchSize = metricRegistry.histogram(name(this.getClass(), "batchSize"));
        this.bufferFlushes = metricRegistry.meter(name(this.getClass(), "bufferFlushes"));
        this.bufferFlushFailures = metricRegistry.meter(name(this.getClass(), "bufferFlushFailures"));
        this.bufferFlushesRequested = metricRegistry.meter(name(this.getClass(), "bufferFlushesRequested"));

        buffer = new ArrayList<>(maxBufferSize);

    }

    @Override
    public void write(Message message) throws Exception {
        for (IndexSet indexSet : message.getIndexSets()) {
            writeMessageEntry(Maps.immutableEntry(indexSet, message));
        }
    }

    public void writeMessageEntry(Map.Entry<IndexSet, Message> entry) throws Exception {
        List<Map.Entry<IndexSet, Message>> flushBatch = null;
        synchronized (this) {
            buffer.add(entry);

            if (buffer.size() >= maxBufferSize) {
                flushBatch = buffer;
                buffer = new ArrayList<>(maxBufferSize);
            }
        }
        // if the current thread found it had to flush any messages, it does so but blocks.
        // this ensures we don't flush more than 'processorCount' in parallel.
        // TODO this will still be time limited by the OutputBufferProcessor and thus be called more often than it should
        if (flushBatch != null) {
            flush(flushBatch);
        }
    }

    private void flush(List<Map.Entry<IndexSet, Message>> messages) {
        // never try to flush an empty buffer
        if (messages.isEmpty()) {
            return;
        }

        activeFlushThreads.incrementAndGet();
        if (log.isDebugEnabled()) {
            log.debug("Starting flushing {} messages, flush threads active {}",
                    messages.size(),
                    activeFlushThreads.get());
        }

        try (Timer.Context ignored = processTime.time()) {
            lastFlushTime.set(System.nanoTime());
            writeMessageEntries(messages);
            batchSize.update(messages.size());
            bufferFlushes.mark();
        } catch (Exception e) {
            log.error("Unable to flush message buffer", e);
            bufferFlushFailures.mark();
        }
        activeFlushThreads.decrementAndGet();
        log.debug("Flushing {} messages completed", messages.size());
    }

    public void forceFlushIfTimedout() {
        // if we shouldn't flush at all based on the last flush time, no need to synchronize on this.
        if (lastFlushTime.get() != 0 &&
                outputFlushInterval > NANOSECONDS.toSeconds(System.nanoTime() - lastFlushTime.get())) {
                    return;
                }
        // flip buffer quickly and initiate flush
        final List<Map.Entry<IndexSet, Message>> flushBatch;
        synchronized (this) {
            flushBatch = buffer;
            buffer = new ArrayList<>(maxBufferSize);
        }
        if (flushBatch != null) {
            bufferFlushesRequested.mark();
            flush(flushBatch);
        }
    }

    public interface Factory extends ElasticSearchOutput.Factory {
    }

    public static class Config extends ElasticSearchOutput.Config {
    }

    public static class Descriptor extends ElasticSearchOutput.Descriptor {
        public Descriptor() {
            super("Blocking Batched Elasticsearch Output", false, "", "Elasticsearch Output with Batching (blocking)");
        }
    }
}
