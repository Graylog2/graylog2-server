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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.cluster.leader.LeaderChangedEvent;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is the default {@link JobSchedulerConfig}.
 */
@Singleton
public class DefaultJobSchedulerConfig implements JobSchedulerConfig {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultJobSchedulerConfig.class);

    private final AtomicBoolean isLeader;

    private final LeaderElectionService leaderElectionService;

    @Inject
    public DefaultJobSchedulerConfig(LeaderElectionService leaderElectionService, EventBus eventBus) {
        this.leaderElectionService = leaderElectionService;

        eventBus.register(this);
        isLeader = new AtomicBoolean(leaderElectionService.isLeader());
    }

    @Override
    public boolean canExecute() {
        return isLeader.get();
    }

    @Override
    public int numberOfWorkerThreads() {
        return 5;
    }

    @Subscribe
    public void leaderChanged(LeaderChangedEvent event) {
        isLeader.set(leaderElectionService.isLeader());
    }
}
