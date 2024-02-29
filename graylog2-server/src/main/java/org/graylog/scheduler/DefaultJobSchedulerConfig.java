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

import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.cluster.leader.LeaderElectionService;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * This is the default {@link JobSchedulerConfig}.
 */
@Singleton
public class DefaultJobSchedulerConfig implements JobSchedulerConfig {
    private final LeaderElectionService leaderElectionService;
    private final JobSchedulerConfiguration config;

    @Inject
    public DefaultJobSchedulerConfig(LeaderElectionService leaderElectionService, JobSchedulerConfiguration config) {
        this.leaderElectionService = leaderElectionService;
        this.config = config;
    }

    @Override
    public boolean canExecute() {
        return leaderElectionService.isLeader();
    }

    @Override
    public int numberOfWorkerThreads() {
        return 5;
    }

    @Override
    @Nullable
    public Map<String, Integer> concurrencyLimits() {
        return ImmutableMap.copyOf(config.getConcurrencyLimits());
    }
}
