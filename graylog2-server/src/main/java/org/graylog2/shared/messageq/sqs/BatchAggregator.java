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
package org.graylog2.shared.messageq.sqs;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Ingests entries until enough of them have accumulated to form a batch of a predefined size. Flushes out the
 * batches by calling a consumer provided on instantiation.
 * @param <T> Type of the batch entries.
 */
public class BatchAggregator<T> {
    private static final Logger log = LoggerFactory.getLogger(BatchAggregator.class);

    private final Consumer<List<T>> flushAction;
    private final int maxBatchSize;
    private final ScheduledExecutorService scheduledExecutorService;

    private List<T> currentBatch;
    private final long flushInterval;
    private volatile long lastFlushTime;
    private volatile boolean isRunning = false;

    /**
     *
     * @param flushAction Consumer which is being called with a batch when flushing
     * @param maxBatchSize Flushing will be triggered once this much entries have accumulated.
     * @param flushInterval To prevent non-full batches being hold back at a low ingest rate, periodical flushes will
     * be initiated at the given interval.
     */
    public BatchAggregator(Consumer<List<T>> flushAction, int maxBatchSize, Duration flushInterval) {
        this.flushAction = flushAction;
        this.maxBatchSize = maxBatchSize;
        this.currentBatch = new ArrayList<>(maxBatchSize);
        this.flushInterval = flushInterval.toNanos();
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("batch-flush-%d")
                        .build());
    }

    /**
     * Start the aggregator so that it is ready to accept entries. This will start the periodical flush thread.
     */
    public void start() {
        if (isRunning) {
            throw new IllegalStateException("Already running.");
        }
        this.scheduledExecutorService.scheduleWithFixedDelay(this::flushIfTimedOut, this.flushInterval,
                this.flushInterval, TimeUnit.NANOSECONDS);
        isRunning = true;
    }

    /**
     * Stops the periodical flush thread and initiates a final flush using the current thread.
     */
    public void shutdown() {
        if (! isRunning) {
            throw new IllegalStateException("Already stopped.");
        }
        isRunning = false;
        scheduledExecutorService.shutdown();
        synchronized (this) {
            if (!currentBatch.isEmpty()) {
                flushAction.accept(currentBatch);
            }
        }
    }

    /**
     * Feed an entry to the aggregator. If this entry triggers a batch to be flushed, the flush action will be called
     * using the current thread.
     * @param entry The entry to add for batching.
     */
    public void feed(T entry) {
        if (!isRunning) {
            throw new IllegalStateException("BatchFlusher is not running.");
        }

        List<T> flushBatch = null;
        synchronized (this) {
            currentBatch.add(entry);
            if (currentBatch.size() >= maxBatchSize) {
                flushBatch = currentBatch;
                currentBatch = new ArrayList<>(maxBatchSize);
            }
            if (flushBatch != null) {
                flush(flushBatch);
            }
        }
    }

    private void flushIfTimedOut() {
        if (lastFlushTime != 0 && flushInterval > System.nanoTime() - lastFlushTime) {
            return;
        }

        final List<T> flushBatch;
        synchronized (this) {
            if (currentBatch.isEmpty()) {
                return;
            }
            flushBatch = currentBatch;
            currentBatch = new ArrayList<>(maxBatchSize);
        }
        flush(flushBatch);
    }

    private void flush(List<T> batch) {
        lastFlushTime = System.nanoTime();
        try {
            flushAction.accept(batch);
        } catch (Exception e) {
            log.error("Error while flushing batch.", e);
        }
    }
}
