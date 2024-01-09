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

import org.graylog2.cluster.lock.LockService;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

public class RefreshingLockServiceFactory {
    private final LockService lockService;
    private final ScheduledExecutorService scheduler;
    private final Duration leaderElectionLockTTL;

    @Inject
    public RefreshingLockServiceFactory(LockService lockService,
                                        @Named("daemonScheduler") ScheduledExecutorService scheduler,
                                        @Named("lock_service_lock_ttl") Duration leaderElectionLockTTL) {

        this.lockService = lockService;
        this.scheduler = scheduler;
        this.leaderElectionLockTTL = leaderElectionLockTTL;
    }

    public RefreshingLockService create() {
        return new RefreshingLockService(lockService, scheduler, leaderElectionLockTTL);
    }
}
