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
package org.graylog.scheduler.worker;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.InstrumentedThreadFactory;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.system.shutdown.GracefulShutdownHook;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Worker pool to execute jobs.
 */
public class JobWorkerPool implements GracefulShutdownHook {
    public interface Factory {
        JobWorkerPool create(String name, int poolSize);
    }

    private static final Logger LOG = LoggerFactory.getLogger(JobWorkerPool.class);
    private static final String NAME_PREFIX = "job-worker-pool";
    private static final String EXECUTOR_NAME = NAME_PREFIX + "-executor";
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9\\-]+");

    private final GracefulShutdownService gracefulShutdownService;
    private final ExecutorService executor;
    private final Semaphore slots;


    @Inject
    public JobWorkerPool(@Assisted String name,
                         @Assisted int poolSize,
                         GracefulShutdownService gracefulShutdownService,
                         MetricRegistry metricRegistry) {
        this.gracefulShutdownService = gracefulShutdownService;
        checkArgument(NAME_PATTERN.matcher(name).matches(), "Pool name must match %s", NAME_PATTERN);

        this.executor = buildExecutor(name, poolSize, metricRegistry);
        this.slots = new Semaphore(poolSize, true);

        registerMetrics(metricRegistry, poolSize);
        gracefulShutdownService.register(this);
    }

    /**
     * Returns the number of free slots in the worker pool.
     *
     * @return numer of free slots
     */
    public int freeSlots() {
        return slots.availablePermits();
    }

    /**
     * Checks if there are free slots in the worker pool
     *
     * @return true if there are free slots, false otherwise
     */
    public boolean hasFreeSlots() {
        return freeSlots() > 0;
    }

    /**
     * Exeute the given job in the worker pool if there are any free slots.
     *
     * @param job the job to execute
     * @return true if the job could be executed, false otherwise
     */
    public boolean execute(final Runnable job) {
        // If there are no available slots, we won't do anything
        final boolean acquired = slots.tryAcquire();
        if (!acquired) {
            return false;
        }

        try {
            executor.execute(() -> {
                try {
                    job.run();
                } catch (Exception e) {
                    LOG.error("Unhandled job execution error", e);
                } finally {
                    slots.release();
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            // This should not happen because we always check the semaphore before submitting jobs to the pool
            slots.release();
            return false;
        }
    }

    @Override
    public void doGracefulShutdown() throws Exception {
        executor.shutdown();
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            LOG.warn("Timeout shutting down worker pool after 60 seconds");
        }
    }

    /**
     * Shutdown the worker pool. This doesn't wait for currently running jobs to complete.
     * Use {@link #awaitTermination(long, TimeUnit)} to do that.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            gracefulShutdownService.unregister(this);
        } catch (IllegalStateException ignore) {
            // Server is already shutting down, we can ignore it
        }
    }

    /**
     * Blocks until all jobs have completed execution after a shutdown request, or the timeout occurs, or the
     * current thread is interrupted, whichever happens first.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return {@code true} if this pool terminated and
     * {@code false} if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        executor.awaitTermination(timeout, unit);
    }

    private static ExecutorService buildExecutor(String name, int poolSize, MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat(NAME_PREFIX + "[" + name + "]-%d")
                .setUncaughtExceptionHandler((t, e) -> LOG.error("Unhandled exception", e))
                .build();
        final InstrumentedThreadFactory itf = new InstrumentedThreadFactory(threadFactory, metricRegistry, name(JobWorkerPool.class, name));
        final SynchronousQueue<Runnable> workQueue = new SynchronousQueue<>();

        final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, poolSize, 60L, TimeUnit.SECONDS, workQueue, itf);
        return new InstrumentedExecutorService(executor, metricRegistry, name(EXECUTOR_NAME, name));
    }

    private void registerMetrics(MetricRegistry metricRegistry, int poolSize) {
        metricRegistry.register(MetricRegistry.name(this.getClass(), "waiting_for_slots"),
                (Gauge<Integer>) () -> slots.getQueueLength());
        metricRegistry.register(MetricRegistry.name(this.getClass(), "free_slots"),
                (Gauge<Integer>) () -> freeSlots());
        metricRegistry.register(MetricRegistry.name(this.getClass(), "total_slots"),
                (Gauge<Integer>) () -> poolSize);

    }
}
