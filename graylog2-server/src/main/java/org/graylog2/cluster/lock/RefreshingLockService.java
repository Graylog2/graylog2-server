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
package org.graylog2.cluster.lock;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Service that can be used to acquire a {@link Lock} that will be refreshed periodically in a background thread.
 * Each instance of the service needs to be closed to release the lock.
 */
public class RefreshingLockService implements AutoCloseable {
    public interface Factory {
        RefreshingLockService create();
    }

    private final LockService lockService;
    private final ScheduledExecutorService scheduler;
    private final Duration lockTTL;
    private ScheduledFuture<?> lockRefreshFuture;
    private Lock lock;

    @Inject
    public RefreshingLockService(LockService lockService,
                                 @Named("daemonScheduler") ScheduledExecutorService scheduler,
                                 @Named("lock_service_lock_ttl") Duration lockTTL) {
        this.lockService = lockService;
        this.scheduler = scheduler;
        this.lockTTL = lockTTL;
    }

    /**
     * Lock the given resource and ensure that only the given number of locks for the resource exist in the cluster.
     *
     * @param resource       the resource to lock
     * @param maxConcurrency maximum number of locks for the resource
     * @throws AlreadyLockedException when the resource couldn't be locked
     */
    public void acquireAndKeepLock(String resource, int maxConcurrency) throws AlreadyLockedException {
        Optional<Lock> optionalLock = lockService.lock(resource, maxConcurrency);
        if (optionalLock.isEmpty()) {
            throw new AlreadyLockedException(f("Could not acquire lock for resource <%s> with max concurrency <%d>", resource, maxConcurrency));
        }
        scheduleLock(optionalLock.get());
    }

    /**
     * Lock the given resource exclusively.
     *
     * @param resource    the resource to lock
     * @param lockContext the identifier for the exclusive lock
     * @throws AlreadyLockedException when the resource couldn't be locked
     */
    public void acquireAndKeepLock(String resource, String lockContext) throws AlreadyLockedException {
        checkArgument(!isNullOrEmpty(lockContext), "lockContext cannot be blank");
        Optional<Lock> optionalLock = lockService.lock(resource, lockContext);
        if (optionalLock.isEmpty()) {
            throw new AlreadyLockedException(f("Could not acquire lock for resource <%s> and lock context <%s>", resource, lockContext));
        }
        scheduleLock(optionalLock.get());
    }

    private void scheduleLock(Lock newLock) {
        lock = newLock;
        Duration duration = lockTTL.minusSeconds(30);
        if (duration.isNegative() || duration.isZero()) {
            duration = Duration.ofSeconds(1);
        }
        lockRefreshFuture = scheduler.scheduleAtFixedRate(() -> refreshLock(lock), duration.toMillis(), duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Release the lock.
     */
    public void releaseLock() {
        if (lockRefreshFuture != null) {
            lockRefreshFuture.cancel(true);
            lockRefreshFuture = null;
        }
        if (lock != null) {
            lockService.unlock(lock);
            lock = null;
        }
    }

    private void refreshLock(Lock lock) {
        final Optional<Lock> newLock = lockService.extendLock(lock);
        if (newLock.isEmpty()) {
            throw new RuntimeException("Failed to refresh lock. This should not happen!");
        }
    }

    /**
     * Release the lock.
     */
    @Override
    public void close() {
        releaseLock();
    }
}
