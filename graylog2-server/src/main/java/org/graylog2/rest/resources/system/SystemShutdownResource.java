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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.system.shutdown.GracefulShutdown;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.accepted;

@RequiresAuthentication
@Api(value = "System/Shutdown", description = "Shutdown this node gracefully.")
@Path("/system/shutdown")
public class SystemShutdownResource extends RestResource {
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
    @ApiOperation(value = "Shutdown this node gracefully.",
            notes = "Attempts to process all buffered and cached messages before exiting, " +
                    "shuts down inputs first to make sure that no new messages are accepted.")
    @Path("/shutdown")
    @AuditEvent(type = AuditEventTypes.NODE_SHUTDOWN_INITIATE)
    public Response shutdown() {
        checkPermission(RestPermissions.NODE_SHUTDOWN, serverStatus.getNodeId().toString());

        new Thread(gracefulShutdown).start();
        return accepted().build();
    }
}
