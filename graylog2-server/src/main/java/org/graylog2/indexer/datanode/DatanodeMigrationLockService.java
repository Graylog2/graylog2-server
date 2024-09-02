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

import org.graylog2.cluster.lock.Lock;
import org.graylog2.indexer.IndexSet;

public interface DatanodeMigrationLockService {
    /**
     * This is a blocking method. It will try to acquire a lock and repeat the process until the general timeout
     * defined in the {@link DatanodeMigrationLockWaitConfig#lockAcquireTimeout()} is reached.
     */
    Lock acquireLock(IndexSet indexSet, Class<?> caller, String context, DatanodeMigrationLockWaitConfig config);

    /**
     * Will run the runnable only if it can get a lock on the first try. If the lock is taken, it will skip
     * the execution.
     */
    void tryRun(IndexSet indexSet, Class<?> caller, Runnable runnable);

    /**
     * Each lock needs to be explicitly released, otherwise the implementation may extend it as long as the
     * node is running.
     */
    void release(Lock lock);
}
