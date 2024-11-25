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
package org.graylog2.indexer.datanode;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.graylog2.cluster.lock.Lock;
import org.graylog2.cluster.lock.LockService;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class DatanodeMigrationLockServiceImpl implements DatanodeMigrationLockService {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeMigrationLockServiceImpl.class);
    public static final int LOCK_EXTEND_PERIOD_SECONDS = 10;
    private static final String LOCK_RESOURCE_PREFIX = "remote-reindex-migration_";

    private final LockService lockService;


    private final Set<Lock> activeLocks = Collections.synchronizedSet(new HashSet<>());

    @Inject
    public DatanodeMigrationLockServiceImpl(LockService lockService) {
        this.lockService = lockService;
        startLocksExtendingThread(lockService);
    }

    /**
     * Locks that are acquired are automatically added to the {@link #activeLocks} collection. These locks
     * will be periodically extended unless explicitly released.
     */
    private void startLocksExtendingThread(LockService lockService) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("migration-locks-service-backend-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler(new Tools.LogUncaughtExceptionHandler(LOG))
                .build();

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
        executorService.scheduleAtFixedRate(
                () -> extendActiveLocks(lockService),
                LOCK_EXTEND_PERIOD_SECONDS,
                LOCK_EXTEND_PERIOD_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /**
     * Extend active locks. The returned value from {@link LockService#extendLock(Lock)} is ignored and not needed.
     * This simplifies the release process, as the original acquired lock is enough for extension and enough for
     * release. If we would update the {@link #activeLocks} collection every time, we'd need to use a smarter
     * strategy to detect which lock should be actually deleted during the release process.
     */
    private synchronized void extendActiveLocks(LockService lockService) {
        final long extendedLocks = activeLocks.stream()
                .map(lockService::extendLock)
                .filter(Optional::isPresent)
                .count();
        if (extendedLocks > 0) {
            LOG.trace("Extended TTL of {} datanode migration locks", extendedLocks);
        }
    }

    @Override
    public Lock acquireLock(IndexSet indexSet, Class<?> caller, String context, DatanodeMigrationLockWaitConfig config) {
        final String indexSetID = indexSet.getConfig().id();
        final String resource = LOCK_RESOURCE_PREFIX + indexSetID;
        return waitForLock(resource, caller, context, indexSet, config);
    }

    @Override
    public void tryRun(IndexSet indexSet, Class<?> caller, Runnable runnable) {
        tryLock(indexSet, caller)
                .ifPresentOrElse(lock -> {
                    try {
                        runnable.run();
                    } finally {
                        release(lock);
                    }
                }, () -> LOG.info("Couldn't enquire a lock of index set {}({}) for {}, skipping execution", indexSet.getConfig().title(), indexSet.getConfig().id(), caller.getName()));
    }

    @Override
    public synchronized void release(Lock lock) {
        activeLocks.remove(lock);
        lockService.unlock(lock);
    }

    private Optional<Lock> tryLock(IndexSet indexSet, Class<?> caller) {
        final String resource = LOCK_RESOURCE_PREFIX + indexSet.getConfig().id();
        return doLock(resource, caller, null);
    }

    private synchronized Optional<Lock> doLock(String resource, Class<?> caller, String context) {
        final Optional<Lock> lock = lockService.lock(resource, caller.getName() + ":" + context);
        lock.ifPresent(activeLocks::add);
        return lock;
    }

    private Lock waitForLock(String resource, Class<?> caller, String context, IndexSet indexSet, DatanodeMigrationLockWaitConfig waitConfig) {
        try {
            return RetryerBuilder.<Optional<Lock>>newBuilder()
                    .withRetryListener(loggingRetryListener(caller, indexSet, waitConfig))
                    .withStopStrategy(StopStrategies.stopAfterDelay(waitConfig.lockAcquireTimeout().toSeconds(), TimeUnit.SECONDS))
                    .withWaitStrategy(WaitStrategies.fixedWait(waitConfig.delayBetweenAttempts().toMillis(), TimeUnit.MILLISECONDS))
                    .retryIfResult(Optional::isEmpty)
                    .build()
                    .call(() -> doLock(resource, caller, context))
                    .orElseThrow(() -> new DatanodeMigrationLockException("Failed to obtain index set " + indexSet.getConfig().title() + " lock"));
        } catch (ExecutionException | RetryException e) {
            throw new DatanodeMigrationLockException("Failed to obtain index set " + indexSet.getConfig().title() + " lock", e);
        }
    }

    @Nonnull
    private static RetryListener loggingRetryListener(Class<?> caller, IndexSet indexSet, DatanodeMigrationLockWaitConfig waitConfig) {
        return new RetryListener() {
            @Override
            public <V> void onRetry(Attempt<V> attempt) {
                waitConfig.lockAcquireListerer().onRetry(indexSet, caller, attempt.getAttemptNumber());
            }
        };
    }
}
