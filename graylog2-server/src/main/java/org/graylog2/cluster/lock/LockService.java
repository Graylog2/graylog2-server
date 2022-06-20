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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public interface LockService {
    /**
     * Request a lock. If a lock already exists, the lock expiry time will be extended.
     *
     * @param resource Unique identifier for the resource that should be guarded by this lock.
     * @param lockContext  an identifier that will be appended to the callers' node id. This will create the lock owner string.
     *                       A context can be used for resources that should only allow a single lock to be acquired, even from the same node.
     *                       If the lockContext is null, only the nodeId will be used.
     * @return A {@link Lock} object, if a lock was obtained. An empty {@link Optional}, if no lock could be acquired.
     */
    Optional<Lock> lock(@Nonnull String resource, @Nullable String lockContext);

    /**
     * Extend the expiry time of an existing lock.
     *
     * @param existingLock the lock that should be extended.
     * @return A {@link Lock} object, if the lock could be extended. An empty {@link Optional}, if no lock extension could be acquired.
     */
    Optional<Lock> extendLock(@Nonnull Lock existingLock);


    Optional<Lock> unlock(@Nonnull String resource, @Nullable String lockContext);
    Optional<Lock> unlock(Lock lock);
}
