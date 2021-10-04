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
package org.graylog2.shared.security;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.cluster.leader.LeaderChangedEvent;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
@Priority(Priorities.AUTHORIZATION)
public class RestrictToMasterFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RestrictToMasterFilter.class);

    private final LeaderElectionService leaderElectionService;
    private final EventBus eventBus;

    private final AtomicBoolean isMaster;

    @Inject
    public RestrictToMasterFilter(LeaderElectionService leaderElectionService, EventBus eventBus) {
        this.leaderElectionService = leaderElectionService;
        this.eventBus = eventBus;

        this.eventBus.register(this);
        isMaster = new AtomicBoolean(leaderElectionService.isLeader());
    }

    @Subscribe
    public void leaderChanged(LeaderChangedEvent event) {
        isMaster.set(leaderElectionService.isLeader());
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!isMaster.get()) {
            LOG.warn("Rejected request to <{}> which is only allowed against master nodes.", requestContext.getUriInfo().getPath());
            throw new ForbiddenException("Request is only allowed against master nodes.");
        }
    }
}
