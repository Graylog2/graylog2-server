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
package org.graylog.scheduler;

import org.graylog2.cluster.lock.Lock;
import org.graylog2.cluster.lock.LockService;
import org.graylog2.shared.utilities.StringUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("AssignmentToNull")
public class RefreshingLockService implements AutoCloseable {
    private final LockService lockService;
    private final ScheduledExecutorService scheduler;
    private final Duration leaderElectionLockTTL;
    private ScheduledFuture<?> lockRefreshFuture;
    private Lock lock;

    public RefreshingLockService(LockService lockService,
                                 ScheduledExecutorService scheduler,
                                 Duration leaderElectionLockTTL) {
        this.lockService = lockService;
        this.scheduler = scheduler;
        this.leaderElectionLockTTL = leaderElectionLockTTL;
    }

    public void acquireAndKeepLock(String resource, int maxConcurrency) throws AlreadyLockedException {
        Optional<Lock> optionalLock = lockService.lock(resource, maxConcurrency);
        if (optionalLock.isEmpty()) {
            throw new AlreadyLockedException(
                    StringUtils.f("Could not acquire lock for resource <%s> with max. concurrency <%d>", resource, maxConcurrency));
        }
        scheduleLock(optionalLock.get());
    }

    public void acquireAndKeepLock(String resource, String triggerId) throws AlreadyLockedException {
        Optional<Lock> optionalLock = lockService.lock(resource, triggerId);
        if (optionalLock.isEmpty()) {
            throw new AlreadyLockedException(StringUtils.f("Could not acquire lock for resource <%s> triggerId <%s>", resource, triggerId));
        }
        scheduleLock(optionalLock.get());
    }

    private void scheduleLock(Lock newLock) {
        lock = newLock;
        Duration duration = leaderElectionLockTTL.minusSeconds(5);
        if (duration.isNegative()) {
            duration = Duration.ofSeconds(1);
        }
        lockRefreshFuture = scheduler.scheduleAtFixedRate(() -> refreshLock(lock), duration.toMillis(), duration.toMillis(), TimeUnit.MILLISECONDS);
    }

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

    @Override
    public void close() {
        releaseLock();
    }
}
