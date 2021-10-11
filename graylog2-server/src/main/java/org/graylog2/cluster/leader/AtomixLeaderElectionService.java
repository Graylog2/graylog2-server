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

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractIdleService;
import io.atomix.cluster.MemberId;
import io.atomix.cluster.discovery.MulticastDiscoveryProvider;
import io.atomix.core.Atomix;
import io.atomix.core.election.LeaderElection;
import io.atomix.core.election.Leadership;
import io.atomix.core.profile.Profile;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AtomixLeaderElectionService extends AbstractIdleService implements LeaderElectionService {
    private final Logger log = LoggerFactory.getLogger(AtomixLeaderElectionService.class);

    private final NodeId nodeId;
    private final EventBus eventBus;
    private final HttpConfiguration httpConfiguration;
    private Atomix atomix;
    private volatile Leadership<MemberId> leadership;


    @Inject
    public AtomixLeaderElectionService(NodeId nodeId, EventBus eventBus, HttpConfiguration httpConfiguration) {
        this.nodeId = nodeId;
        this.eventBus = eventBus;
        this.httpConfiguration = httpConfiguration;
    }

    @Override
    protected void startUp() throws Exception {
        eventBus.register(this);

        atomix = Atomix.builder()
                .withMemberId(nodeId.toString())
                .withAddress(httpConfiguration.getHttpBindAddress().getHost())
                .withClusterId("test")
                .withMembershipProvider(MulticastDiscoveryProvider.builder().build())
                .withProfiles(Profile.consensus())
                .build();
        atomix.start().join();

        LeaderElection<MemberId> election = atomix.getLeaderElection("graylog-leader-election");
        leadership = election.run(atomix.getMembershipService().getLocalMember().id());

        if (isLeader()) {
            System.out.println("XXX - I am the leader!");
        }
    }

    @Override
    protected void shutDown() throws Exception {
        eventBus.unregister(this);
    }

    @Override
    public boolean isLeader() {
        if (leadership == null) {
            return false;
        }
        return leadership.leader().id().equals(atomix.getMembershipService().getLocalMember().id());
    }
}
