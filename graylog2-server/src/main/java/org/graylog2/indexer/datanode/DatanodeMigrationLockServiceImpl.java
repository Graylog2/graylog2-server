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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class DatanodeMigrationLockServiceImpl implements DatanodeMigrationLockService {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeMigrationLockServiceImpl.class);
    public static final int ACQUIRE_LOCK_SLEEP_MILLIS = 100;
    public static final int LOCK_EXTEND_PERIOD_SECONDS = 10;
    private static final String LOCK_RESOURCE_PREFIX = "remote-reindex-migration_";

    private final LockService lockService;


    private final ScheduledExecutorService executorService;

    private final Set<Lock> activeLocks = Collections.synchronizedSet(new HashSet<>());

    @Inject
    public DatanodeMigrationLockServiceImpl(LockService lockService) {
        this.lockService = lockService;
        this.executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("migration-locks-service-backend-%d").setDaemon(true).setUncaughtExceptionHandler(new Tools.LogUncaughtExceptionHandler(LOG)).build());
        this.executorService.scheduleAtFixedRate(() -> {
            final Set<Optional<Lock>> extendedLocks = activeLocks.stream().map(lockService::extendLock).collect(Collectors.toSet());
            if (!extendedLocks.isEmpty()) {
                LOG.info("Extended TTL of {} datanode migration locks", extendedLocks.size());
            }
        }, LOCK_EXTEND_PERIOD_SECONDS, LOCK_EXTEND_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    private Optional<Lock> tryLock(IndexSet indexSet, Class<?> caller) {
        final String resource = LOCK_RESOURCE_PREFIX + indexSet.getConfig().id();
        return doLock(resource, caller);
    }

    private Optional<Lock> doLock(String resource, Class<?> caller) {
        final Optional<Lock> lock = lockService.lock(resource, caller.getName());
        lock.ifPresent(activeLocks::add);
        return lock;
    }

    @Override
    public void release(Lock lock) {
        activeLocks.remove(lock);
        lockService.unlock(lock);
    }

    @Override
    public Lock acquireLock(IndexSet indexSet, Class<?> caller) {
        final String indexSetID = indexSet.getConfig().id();
        final String resource = LOCK_RESOURCE_PREFIX + indexSetID;
        return lock(resource, caller);
    }

    private Lock lock(String resource, Class<?> caller) {

        Optional<Lock> lock;
        while ((lock = doLock(resource, caller)).isEmpty()) {
            try {
                LOG.info("Caller {} is Waiting for a lock {}, retrying in 100ms", caller.getName(), resource);
                Thread.sleep(ACQUIRE_LOCK_SLEEP_MILLIS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return lock.get();
    }

    @Override
    public void tryRun(IndexSet indexSet, Class<?> caller, Runnable runnable) {
        final Optional<Lock> lock = tryLock(indexSet, caller);
        // here we have to keep the lock refreshed every now and then
        lock.ifPresentOrElse(l -> {
            try {
                runnable.run();
            } finally {
                release(l);
            }
        }, () -> LOG.info("Couldn't enquire a lock of index set {}({}) for {}, skipping execution", indexSet.getConfig().title(), indexSet.getConfig().id(), caller.getName()));
    }
}
