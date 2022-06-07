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
package org.graylog.scheduler.capabilities;

import com.google.common.collect.ImmutableSet;
import org.graylog2.cluster.leader.LeaderElectionService;

import javax.inject.Inject;
import java.util.Set;

public class ServerNodeSchedulerCapabilities implements SchedulerCapabilities {

    public static final String IS_LEADER = "org.graylog.cluster.is-leader";
    private final LeaderElectionService leaderElectionService;

    @Inject
    public ServerNodeSchedulerCapabilities(LeaderElectionService leaderElectionService) {
        this.leaderElectionService = leaderElectionService;
    }

    @Override
    public Set<String> getNodeCapabilities() {
        if (leaderElectionService.isLeader()) {
            return ImmutableSet.of(IS_LEADER);
        }
        return ImmutableSet.of();
    }
}
