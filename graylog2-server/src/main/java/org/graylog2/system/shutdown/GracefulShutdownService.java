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
package org.graylog2.system.shutdown;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

/**
 * A service that participates in the Graylog server graceful shutdown.
 * <p>
 * Services can implement {@link GracefulShutdownHook} and register themselves with this service to make sure they
 * get shut down properly on server shutdown. During shutdown the registered hooks will be called in no particular
 * order.
 * <p>
 * Make sure to use {@link #unregister(GracefulShutdownHook)} if a registered service is shutting down before the
 * server shutdown to avoid leaking service instances in the {@link GracefulShutdownService}.
 *
 * See {@link GracefulShutdownHook} for an example.
 */
@Singleton
public class GracefulShutdownService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(GracefulShutdownService.class);

    private final Set<GracefulShutdownHook> shutdownHooks = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    @Override
    protected void startUp() {
        // Nothing to do
    }

    @Override
    protected void shutDown() {
        // Don't do anything if the shutdown is already in progress or if there are no hooks registered
        if (isShuttingDown.getAndSet(true) || shutdownHooks.isEmpty()) {
            return;
        }

        try {
            // Use an executor to run the shutdown hooks in parallel but don't start too many threads
            // TODO: Make max number of threads user configurable
            final ExecutorService executor = executorService(Math.min(shutdownHooks.size(), 10));
            final CountDownLatch latch = new CountDownLatch(shutdownHooks.size());

            LOG.info("Running graceful shutdown for <{}> shutdown hooks", shutdownHooks.size());
            for (final GracefulShutdownHook shutdownHook : shutdownHooks) {
                executor.submit(() -> {
                    final String hookName = shutdownHook.getClass().getSimpleName();
                    try {
                        LOG.info("Initiate shutdown for <{}>", hookName);
                        final Stopwatch stopwatch = Stopwatch.createStarted();
                        shutdownHook.doGracefulShutdown();
                        LOG.info("Finished shutdown for <{}>, took {} ms", hookName, stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
                    } catch (Exception e) {
                        LOG.error("Problem shutting down <{}>", hookName, e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();
        } catch (Exception e) {
            LOG.error("Problem shutting down registered hooks", e);
        }
    }

    /**
     * Register a shutdown hook with the service.
     * @param shutdownHook a class that implements {@link GracefulShutdownHook}
     * @throws IllegalStateException if the server shutdown is already in progress and the hook cannot be registered
     * @throws NullPointerException if the shutdown hook argument is null
     */
    public void register(GracefulShutdownHook shutdownHook) {
        if (isShuttingDown.get()) {
            // Avoid any changes to the shutdown hooks set when the shutdown is already in progress
            throw new IllegalStateException("Couldn't register shutdown hook because shutdown is already in progress");
        }
        shutdownHooks.add(requireNonNull(shutdownHook, "shutdownHook cannot be null"));
    }

    /**
     * Remove a previously registered shutdown hook from the service.
     * <p>
     * This needs to be called if a registered service will be stopped before the server shuts down.
     * @param shutdownHook a class that implements {@link GracefulShutdownHook}
     * @throws IllegalStateException if the server shutdown is already in progress and the hook cannot be unregistered
     * @throws NullPointerException if the shutdown hook argument is null
     */
    public void unregister(GracefulShutdownHook shutdownHook) {
        if (isShuttingDown.get()) {
            // Avoid any changes to the shutdown hooks set when the shutdown is already in progress
            throw new IllegalStateException("Couldn't unregister shutdown hook because shutdown is already in progress");
        }
        shutdownHooks.remove(requireNonNull(shutdownHook, "shutdownHook cannot be null"));
    }

    private ExecutorService executorService(final int maxThreads) {
        return new ThreadPoolExecutor(0,
                maxThreads,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadFactoryBuilder()
                        .setNameFormat("graceful-shutdown-service-%d")
                        .setUncaughtExceptionHandler((t, e) -> LOG.error("Uncaught exception in <{}>", t, e))
                        .build());
    }
}
