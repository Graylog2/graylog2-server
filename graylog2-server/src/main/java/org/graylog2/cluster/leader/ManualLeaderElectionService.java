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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ManualLeaderElectionService extends AbstractIdleService implements LeaderElectionService {
    private final Logger log = LoggerFactory.getLogger(ManualLeaderElectionService.class);

    private final ClusterConfigService clusterConfigService;
    private final NodeId nodeId;
    private final EventBus eventBus;


    @Inject
    public ManualLeaderElectionService(ClusterConfigService clusterConfigService, NodeId nodeId, EventBus eventBus) {
        this.clusterConfigService = clusterConfigService;
        this.nodeId = nodeId;
        this.eventBus = eventBus;
    }

    public void promoteSelf() {
        promoteNode(nodeId.toString());
    }

    public void promoteNode(String nodeId) {
        clusterConfigService.write(Leader.create(nodeId));
    }

    @Override
    public boolean isLeader() {
        final Leader leader = clusterConfigService.get(Leader.class);
        if (leader == null) {
            return false;
        }
        return leader.nodeId().equals(nodeId.toString());
    }

    @Override
    protected void startUp() throws Exception {
        eventBus.register(this);
    }

    @Override
    protected void shutDown() throws Exception {
        eventBus.unregister(this);
    }

    @Subscribe
    public void clusterConfigChanged(ClusterConfigChangedEvent event) {
        if (event.type().equals(Leader.class.getCanonicalName())) {
            final Leader leader = clusterConfigService.get(Leader.class);

            if (leader == null) {
                throw new IllegalStateException("Cannot find leader in cluster config after event.");
            }

            eventBus.post(LeaderChangedEvent.create(leader.nodeId()));
        }
    }

    @AutoValue
    public static abstract class Leader {
        @JsonProperty("node_id")
        public abstract String nodeId();

        @JsonCreator
        public static Leader create(@JsonProperty("node_id") String nodeId) {
            return new AutoValue_ManualLeaderElectionService_Leader(nodeId);
        }
    }
}
