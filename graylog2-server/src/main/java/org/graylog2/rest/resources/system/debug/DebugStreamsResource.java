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
package org.graylog2.rest.resources.system.debug;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.streams.StreamRouter;

import jakarta.inject.Inject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@RequiresAuthentication
@Tag(name = "System/Debug/Streams", description = "For debugging local and cluster events.")
@Path("/system/debug/streams")
@Produces(MediaType.APPLICATION_JSON)
public class DebugStreamsResource extends RestResource {

    private final StreamRouter streamRouter;

    @Inject
    public DebugStreamsResource(StreamRouter streamRouter) {
        this.streamRouter = streamRouter;
    }

    @GET
    @Path("/router_engine_info")
    @Operation(summary = "Get information about currently active stream router engine.")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEngineFingerprint() {
        return Response.status(Response.Status.OK)
                .entity(streamRouter.getRouterEngineInfo())
                .build();
    }
}
