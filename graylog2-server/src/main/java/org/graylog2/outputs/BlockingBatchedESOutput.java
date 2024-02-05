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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.messages.IndexingResults;
import org.graylog2.indexer.messages.MessageWithIndex;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Message;
import org.graylog2.shared.journal.Journal;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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
    private final Cluster cluster;
    private final int shutdownTimeoutMs;
    private final ScheduledExecutorService daemonScheduler;

    private volatile List<MessageWithIndex> buffer;

    private static final AtomicInteger activeFlushThreads = new AtomicInteger(0);
    private final AtomicLong lastFlushTime = new AtomicLong();
    private final int outputFlushInterval;
    private ScheduledFuture<?> flushTask;

    @Inject
    public BlockingBatchedESOutput(MetricRegistry metricRegistry,
                                   Messages messages,
                                   org.graylog2.Configuration serverConfiguration,
                                   Journal journal,
                                   MessageQueueAcknowledger acknowledger,
                                   Cluster cluster,
                                   @Named("daemonScheduler") ScheduledExecutorService daemonScheduler) {
        super(metricRegistry, messages, journal, acknowledger);
        this.maxBufferSize = serverConfiguration.getOutputBatchSize();
        outputFlushInterval = serverConfiguration.getOutputFlushInterval();
        this.processTime = metricRegistry.timer(name(this.getClass(), "processTime"));
        this.batchSize = metricRegistry.histogram(name(this.getClass(), "batchSize"));
        this.bufferFlushes = metricRegistry.meter(name(this.getClass(), "bufferFlushes"));
        this.bufferFlushFailures = metricRegistry.meter(name(this.getClass(), "bufferFlushFailures"));
        this.bufferFlushesRequested = metricRegistry.meter(name(this.getClass(), "bufferFlushesRequested"));
        this.cluster = cluster;
        this.shutdownTimeoutMs = serverConfiguration.getShutdownTimeout();
        this.daemonScheduler = daemonScheduler;

        buffer = new ArrayList<>(maxBufferSize);
    }

    @Override
    public void write(Message message) throws Exception {
        for (IndexSet indexSet : message.getIndexSets()) {
            writeMessageEntry(new MessageWithIndex(message, indexSet));
        }
    }

    public void writeMessageEntry(MessageWithIndex entry) throws Exception {
        List<MessageWithIndex> flushBatch = null;
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

    private void flush(List<MessageWithIndex> messages) {
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

        try {
            indexMessageBatch(messages);
            // This does not exclude failedMessageIds, because we don't know if ES is ever gonna accept these messages.
            acknowledger.acknowledge(messages.stream().map(MessageWithIndex::message).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Unable to flush message buffer", e);
            bufferFlushFailures.mark();
        }
        activeFlushThreads.decrementAndGet();
        log.debug("Flushing {} messages completed", messages.size());
    }

    protected IndexingResults indexMessageBatch(List<MessageWithIndex> messages) throws Exception {
        try (Timer.Context ignored = processTime.time()) {
            lastFlushTime.set(System.nanoTime());
            final IndexingResults indexingResults = writeMessageEntries(messages);
            batchSize.update(messages.size());
            bufferFlushes.mark();
            return indexingResults;
        }
    }

    public void forceFlushIfTimedout() {
        // if we shouldn't flush at all based on the last flush time, no need to synchronize on this.
        if (lastFlushTime.get() != 0 &&
                outputFlushInterval > NANOSECONDS.toSeconds(System.nanoTime() - lastFlushTime.get())) {
            return;
        }

        forceFlush();
    }

    private void forceFlush() {
        // flip buffer quickly and initiate flush
        final List<MessageWithIndex> flushBatch;
        synchronized (this) {
            flushBatch = buffer;
            buffer = new ArrayList<>(maxBufferSize);
        }
        if (flushBatch != null) {
            bufferFlushesRequested.mark();
            flush(flushBatch);
        }
    }

    @Override
    public void stop() {
        if (flushTask != null) {
            flushTask.cancel(false);
        }

        if (cluster.isConnected() && cluster.isDeflectorHealthy()) {
            // Try to flush current batch. Time-limited to avoid blocking shutdown too long.
            final ExecutorService executorService = Executors.newSingleThreadExecutor(
                    new ThreadFactoryBuilder().setNameFormat("es-output-shutdown-flush").build());
            try {
                executorService.submit(this::forceFlush).get(shutdownTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // OK, we are shutting down anyway
            } catch (ExecutionException e) {
                log.warn("Flushing current batch to indexer while stopping failed with message: {}.", e.getMessage());
            } catch (TimeoutException e) {
                log.warn("Timed out flushing current batch to indexer while stopping.");
            } finally {
                executorService.shutdownNow();
            }
        }
        super.stop();
    }

    @Override
    public void initialize() throws Exception {
        this.flushTask = daemonScheduler.scheduleAtFixedRate(() -> {
                    try {
                        forceFlushIfTimedout();
                    } catch (Exception e) {
                        log.error("Caught exception while trying to flush output", e);
                    }
                },
                outputFlushInterval, outputFlushInterval, TimeUnit.SECONDS);
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
