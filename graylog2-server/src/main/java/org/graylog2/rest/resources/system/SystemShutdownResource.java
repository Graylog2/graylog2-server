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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.system.shutdown.GracefulShutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.accepted;

/**
 * @deprecated shutting down the node using an API request is discouraged in favor of using a service manager to
 * control the server process.
 */
@RequiresAuthentication
@Path("/system/shutdown")
@Produces(MediaType.APPLICATION_JSON)
@Deprecated
public class SystemShutdownResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SystemShutdownResource.class);

    private final GracefulShutdown gracefulShutdown;
    private final ServerStatus serverStatus;

    @Inject
    public SystemShutdownResource(GracefulShutdown gracefulShutdown,
                                  ServerStatus serverStatus) {
        this.gracefulShutdown = gracefulShutdown;
        this.serverStatus = serverStatus;
    }

    @POST
    @Timed
    @Path("/shutdown")
    @AuditEvent(type = AuditEventTypes.NODE_SHUTDOWN_INITIATE)
    @Deprecated
    public Response shutdown() {
        checkPermission(RestPermissions.NODE_SHUTDOWN, serverStatus.getNodeId().toString());

        new Thread(gracefulShutdown).start();

        final String msg =
                "Deprecated API endpoint /system/shutdown/shutdown was called. Shutting down the node via the API is " +
                        "discouraged in favor of using a service manager to control the server process.";
        LOG.warn(msg);
        return accepted(ImmutableMap.of("message", msg)).build();
    }
}
