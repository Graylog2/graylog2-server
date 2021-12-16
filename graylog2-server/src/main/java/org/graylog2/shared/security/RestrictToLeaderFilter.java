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

@Singleton
@Priority(Priorities.AUTHORIZATION)
public class RestrictToLeaderFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RestrictToLeaderFilter.class);

    private final LeaderElectionService leaderElectionService;

    @Inject
    public RestrictToLeaderFilter(LeaderElectionService leaderElectionService) {
        this.leaderElectionService = leaderElectionService;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!leaderElectionService.isLeader()) {
            LOG.warn("Rejected request to <{}> which is only allowed against leader nodes.", requestContext.getUriInfo().getPath());
            throw new ForbiddenException("Request is only allowed against leader nodes.");
        }
    }
}
