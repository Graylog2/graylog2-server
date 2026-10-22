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
package org.graylog2.rest.resources.system.indexer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import static org.graylog2.metrics.entity.EntityMetricsModule.ENTITY_TYPE_INDEX_SETS;

@RequiresAuthentication
@Tag(name = "System/IndexSets/Metrics", description = "Index set metrics")
@Path("/system/indices/index_sets/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class IndexSetMetricsResource extends RestResource {

    private final EntityMetricsService metricsService;

    @Inject
    public IndexSetMetricsResource(@Named(ENTITY_TYPE_INDEX_SETS) EntityMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GET
    @Operation(summary = "Get metrics for multiple index sets")
    public EntityMetricsResponse getMetrics(
            @Parameter(description = "List of index set IDs", required = true)
            @QueryParam("index_set_ids") List<String> indexSetIds,
            @Parameter(description = "List of metric fields to return", required = true)
            @QueryParam("fields") List<String> fields,
            @Context SearchUser searchUser) {

        indexSetIds.forEach(indexSetId -> checkPermission(RestPermissions.INDEXSETS_READ, indexSetId));

        return EntityMetricsResponse.fromValues(
                metricsService.getMetrics(indexSetIds, Set.copyOf(fields), searchUser));
    }
}
