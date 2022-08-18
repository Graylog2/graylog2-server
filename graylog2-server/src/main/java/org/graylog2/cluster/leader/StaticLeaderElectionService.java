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
package org.graylog2.cluster.leader;

import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.Configuration;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Leader election based on the static {@link Configuration#isLeader()} setting in the configuration file. After node
 * startup, the leader status is static and will not change.
 * <p>
 * If the value of {@link Configuration#isLeader()} is false, the node will not be a leader, if the value is true, the
 * node will be a leader, unless the node notices upon startup that there is another leader present in the cluster
 * already. In that case the value will be set to false and the node will act as a follower.
 */
@Singleton
public class StaticLeaderElectionService extends AbstractIdleService implements LeaderElectionService {
    private final Configuration configuration;

    @Inject
    public StaticLeaderElectionService(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean isLeader() {
        return configuration.isLeader();
    }

    @Override
    protected void startUp() throws Exception {
    }

    @Override
    protected void shutDown() throws Exception {
        // This has likely no effect in the server shutdown phase
        configuration.setIsLeader(false);
    }
}
