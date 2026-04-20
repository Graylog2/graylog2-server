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

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.indexer.IndexFailure;
import org.graylog2.indexer.IndexFailureService;
import org.graylog2.rest.models.system.indexer.responses.FailureCount;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiresAuthentication
@Tag(name = "Indexer/Failures", description = "Indexer failures")
@Path("/system/indexer/failures")
public class FailuresResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(FailuresResource.class);

    private IndexFailureService indexFailureService;

    @Inject
    public FailuresResource(IndexFailureService indexFailureService) {
        this.indexFailureService = indexFailureService;
    }

    @GET
    @Timed
    @Operation(summary = "Total count of failed index operations since the given date.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid date parameter provided.")
    })
    @RequiresPermissions(RestPermissions.INDICES_FAILURES)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("count")
    public FailureCount count(@Parameter(name = "since", description = "ISO8601 date", required = true)
                              @QueryParam("since") @NotEmpty String since) {
        final DateTime sinceDate;
        try {
            sinceDate = DateTime.parse(since);
        } catch (IllegalArgumentException e) {
            final String msg = "Invalid date parameter provided: [" + since + "]";
            LOG.error(msg, e);
            throw new BadRequestException(msg);
        }

        return FailureCount.create(indexFailureService.countSince(sinceDate));
    }

    @GET
    @Timed
    @Operation(summary = "Get a list of failed index operations.")
    @RequiresPermissions(RestPermissions.INDICES_FAILURES)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> single(@Parameter(name = "limit", description = "Limit", required = true)
                                      @QueryParam("limit") @Min(0) int limit,
                                      @Parameter(name = "offset", description = "Offset", required = true)
                                      @QueryParam("offset") @Min(0) int offset) {
        final List<IndexFailure> indexFailures = indexFailureService.all(limit, offset);
        final List<Map<String, Object>> failures = new ArrayList<>(indexFailures.size());
        for (IndexFailure failure : indexFailures) {
            failures.add(failure.asMap());
        }

        return ImmutableMap.of(
                "failures", failures,
                "total", indexFailureService.totalCount());
    }
}
