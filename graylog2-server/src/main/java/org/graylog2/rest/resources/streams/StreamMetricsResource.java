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
package org.graylog2.rest.resources.streams;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.metrics.entity.EntityMetricsResponse;
import org.graylog2.metrics.entity.EntityMetricsService;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.util.List;
import java.util.Set;

import static org.graylog2.metrics.entity.EntityMetricsModule.ENTITY_TYPE_STREAMS;

@RequiresAuthentication
@Path("/streams/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class StreamMetricsResource extends RestResource {

    private final EntityMetricsService metricsService;

    @Inject
    public StreamMetricsResource(@Named(ENTITY_TYPE_STREAMS) EntityMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GET
    @Operation(summary = "Get metrics for multiple streams")
    public EntityMetricsResponse getMetrics(
            @Parameter(description = "List of stream IDs", required = true)
            @QueryParam("stream_ids") List<String> streamIds,
            @Parameter(description = "List of metric fields to return", required = true)
            @QueryParam("fields") List<String> fields,
            @Context SearchUser searchUser) {

        streamIds.forEach(streamId -> checkPermission(RestPermissions.STREAMS_READ, streamId));

        return EntityMetricsResponse.fromValues(
                metricsService.getMetrics(streamIds, Set.copyOf(fields), searchUser));
    }
}
