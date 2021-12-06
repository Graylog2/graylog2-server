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

import org.graylog2.cluster.leader.LeaderElectionService;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This is the default {@link JobSchedulerConfig}.
 */
@Singleton
public class DefaultJobSchedulerConfig implements JobSchedulerConfig {
    private final LeaderElectionService leaderElectionService;

    @Inject
    public DefaultJobSchedulerConfig(LeaderElectionService leaderElectionService) {
        this.leaderElectionService = leaderElectionService;
    }

    @Override
    public boolean canExecute() {
        return leaderElectionService.isLeader();
    }

    @Override
    public int numberOfWorkerThreads() {
        return 5;
    }
}
