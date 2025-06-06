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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.outputs.filter.FilteredMessage;
import org.graylog2.outputs.filter.OutputFilter;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.outputs.FilteredMessageOutput;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.graylog2.system.shutdown.GracefulShutdownHook;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * A message output that filters incoming messages and writes batches of filtered messages to all
 * registered {@link FilteredMessageOutput} outputs.
 */
@Singleton
public class BatchedMessageFilterOutput implements MessageOutput, GracefulShutdownHook {
    private static final Logger LOG = LoggerFactory.getLogger(BatchedMessageFilterOutput.class);

    private final Map<String, FilteredMessageOutput> outputs;
    private final OutputFilter outputFilter;
    private final Duration outputFlushInterval;
    private final Duration shutdownTimeout;
    private final ScheduledExecutorService daemonScheduler;

    private final AtomicInteger activeFlushThreads = new AtomicInteger(0);
    private final Histogram batchSize;
    private final Meter bufferFlushes;
    private final Meter bufferFlushFailures;
    private final Meter bufferFlushesRequested;
    private final Cluster cluster;
    private final MessageQueueAcknowledger acknowledger;
    private final Meter outputWriteFailures;
    private final Timer processTime;
    private final GracefulShutdownService gracefulShutdownService;
    private final IndexSetAwareMessageOutputBuffer buffer;
    private ScheduledFuture<?> flushTask;

    @Inject
    public BatchedMessageFilterOutput(Map<String, FilteredMessageOutput> outputs,
                                      OutputFilter outputFilter,
                                      MetricRegistry metricRegistry,
                                      Cluster cluster,
                                      MessageQueueAcknowledger acknowledger,
                                      IndexSetAwareMessageOutputBuffer indexSetAwareMessageOutputBuffer,
                                      GracefulShutdownService gracefulShutdownService,
                                      @Named("output_flush_interval") int outputFlushInterval,
                                      @Named("shutdown_timeout") int shutdownTimeoutMs,
                                      @Named("daemonScheduler") ScheduledExecutorService daemonScheduler) {
        if (outputs.isEmpty()) {
            // We want to fail hard if we don't have any outputs!
            throw new IllegalStateException("No registered outputs found!");
        }

        this.outputs = outputs;
        this.outputFilter = outputFilter;
        this.cluster = cluster;
        this.acknowledger = acknowledger;
        this.outputFlushInterval = Duration.ofSeconds(outputFlushInterval);
        this.shutdownTimeout = Duration.ofMillis(shutdownTimeoutMs);
        this.daemonScheduler = daemonScheduler;
        this.buffer = indexSetAwareMessageOutputBuffer;

        this.batchSize = metricRegistry.histogram(name(this.getClass(), "batchSize"));
        this.bufferFlushes = metricRegistry.meter(name(this.getClass(), "bufferFlushes"));
        this.bufferFlushFailures = metricRegistry.meter(name(this.getClass(), "bufferFlushFailures"));
        this.bufferFlushesRequested = metricRegistry.meter(name(this.getClass(), "bufferFlushesRequested"));
        this.processTime = metricRegistry.timer(name(this.getClass(), "processTime"));
        this.outputWriteFailures = metricRegistry.meter(name(this.getClass(), "outputWriteFailures"));
        this.gracefulShutdownService = gracefulShutdownService;
    }

    @Override
    public void initialize() throws Exception {
        LOG.debug("Starting buffer flush task to run every {} milliseconds", outputFlushInterval.toMillis());
        this.flushTask = daemonScheduler.scheduleAtFixedRate(() -> {
            try {
                if (buffer.shouldFlush(outputFlushInterval)) {
                    forceFlush();
                }
            } catch (Exception e) {
                LOG.error("Caught exception while trying to flush outputs", e);
            }
        }, outputFlushInterval.toMillis(), outputFlushInterval.toMillis(), TimeUnit.MILLISECONDS);
        gracefulShutdownService.register(this);
    }

    @VisibleForTesting
    void forceFlush() {
        LOG.debug("Force-flushing the buffer");
        bufferFlushesRequested.mark();
        buffer.flush(this::flush);
    }

    private void flush(List<FilteredMessage> filteredMessages) {
        if (filteredMessages.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Skipping buffer flush with empty buffer");
            }
            return;
        }

        batchSize.update(filteredMessages.size());

        activeFlushThreads.incrementAndGet();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting flushing {} messages, flush threads active {}",
                    filteredMessages.size(),
                    activeFlushThreads.get());
        }

        try (var ignored = processTime.time()) {
            for (final var output : outputs.values()) {
                try {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Writing {} message(s) to output <{}>", filteredMessages.size(), output);
                    }
                    output.writeFiltered(filteredMessages);
                } catch (Exception e) {
                    LOG.error("Couldn't write {} message(s) to output <{}>", filteredMessages.size(), output.getClass(), e);
                    outputWriteFailures.mark();
                }
            }

            bufferFlushes.mark();
            // We only acknowledge messages once all outputs are done writing messages.
            acknowledger.acknowledge(filteredMessages.stream().map(FilteredMessage::message).toList());
        } catch (Exception e) {
            LOG.error("Error while flushing messages", e);
            bufferFlushFailures.mark();
        }

        activeFlushThreads.decrementAndGet();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Flushing {} messages completed", filteredMessages.size());
        }
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Writing {} messages", messages.size());
        }
        for (final var message : messages) {
            write(message);
        }
    }

    @Override
    public void write(Message message) throws Exception {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Running output filter on message: {}", message);
        }
        final var filteredMessage = outputFilter.apply(message);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Appending filtered message <{}> to buffer", filteredMessage);
        }
        buffer.appendAndFlush(filteredMessage, this::flush);
    }

    @VisibleForTesting
    void cancelFlushTask() {
        if (flushTask != null) {
            LOG.debug("Cancelling flush task");
            flushTask.cancel(false);
        }
    }

    @Override
    public void stop() {
        LOG.debug("Stopping output filter");
        doGracefulShutdown();
        try {
            gracefulShutdownService.unregister(this);
        } catch (IllegalStateException e) {
            LOG.debug("Couldn't unregister from graceful shutdown service: {}", e.getMessage());
        }
    }

    @Override
    public void doGracefulShutdown() {
        cancelFlushTask();

        if (cluster.isConnected() && cluster.isDeflectorHealthy()) {
            // Try to flush current batch. Time-limited to avoid blocking shutdown too long.
            final ExecutorService executorService = Executors.newSingleThreadExecutor(
                    new ThreadFactoryBuilder().setNameFormat("batched-message-filter-output-shutdown-flush").build());
            try {
                LOG.debug("Flushing the current buffer for shutdown");
                executorService.submit(this::forceFlush).get(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // OK, we are shutting down anyway
            } catch (ExecutionException e) {
                LOG.warn("Flushing current message batch to outputs while stopping failed: {}.", e.getMessage());
            } catch (TimeoutException e) {
                LOG.warn("Timed out flushing current batch to outputs while stopping.");
            } finally {
                executorService.shutdownNow();
            }
        }
    }
}
