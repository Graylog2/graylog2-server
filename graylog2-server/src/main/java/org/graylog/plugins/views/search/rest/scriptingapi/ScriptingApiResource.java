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
package org.graylog.plugins.views.search.rest.scriptingapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.QueryFailedException;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.QueryParamsToFullRequestSpecificationMapper;
import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.MessagesRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.utilities.StringUtils;

import java.util.List;
import java.util.Set;

import static org.graylog2.shared.utilities.StringUtils.splitByComma;

@PublicCloudAPI
@Tag(name = "Search/Simple", description = "Simple search API for aggregating and messages retrieval")
@Path("/search")
@Consumes({MediaType.APPLICATION_JSON})
@RequiresAuthentication
public class ScriptingApiResource extends RestResource implements PluginRestResource {
    private final ScriptingApiService service;
    private final QueryParamsToFullRequestSpecificationMapper queryParamsToFullRequestSpecificationMapper;

    @Inject
    public ScriptingApiResource(final ScriptingApiService service,
                                final QueryParamsToFullRequestSpecificationMapper queryParamsToFullRequestSpecificationMapper) {
        this.service = service;
        this.queryParamsToFullRequestSpecificationMapper = queryParamsToFullRequestSpecificationMapper;
    }

    @POST
    @Operation(summary = "Execute query specified by `queryRequestSpec`",
                  operationId = "messagesByQueryRequestSpec")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Query executed successfully",
                    content = @Content(schema = @Schema(implementation = TabularResponse.class)))
    })
    @Path("messages")
    @NoAuditEvent("Creating audit event manually in method body.")
    public TabularResponse executeQuery(@Parameter(name = "queryRequestSpec") @Valid MessagesRequestSpec messagesRequestSpec,
                                        @Context SearchUser searchUser) {
        try {
            return service.executeQuery(messagesRequestSpec, searchUser);
        } catch (IllegalArgumentException | ValidationException | QueryFailedException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    @GET
    @Operation(summary = "Execute query specified by query parameters", operationId = "messagesByQueryParameters")
    @Path("messages")
    @NoAuditEvent("Creating audit event manually in method body.")
    public TabularResponse executeQuery(@Parameter(name = "query", description = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
                                        @Parameter(name = "streams", description = "Comma separated list of streams to search in") Set<String> streams,
                                        @Parameter(name = "stream_categories", description = "Comma separated list of streams categories to search in") @QueryParam("stream_categories") Set<String> streamCategories,
                                        @Parameter(name = "timerange", description = "Timeframe to search in. See method description.", required = true) @QueryParam("timerange") String timerangeKeyword,
                                        @Parameter(name = "fields", description = "Fields from the message to show as columns in result") @QueryParam("fields") List<String> fields,
                                        @Parameter(name = "sort", description = "Field to sort on") @QueryParam("sort") String sort,
                                        @Parameter(name = "sortOrder", description = "Sort order - asc/desc",
                                                  schema = @Schema(allowableValues = {"asc", "desc"}))
                                        @QueryParam("sortOrder") SortSpec.Direction sortOrder,
                                        @Parameter(name = "from", description = "For paging results. Starting from result") @QueryParam("from") int from,
                                        @Parameter(name = "size", description = "Page size") @QueryParam("size") int size,
                                        @Context SearchUser searchUser) {

        try {
            MessagesRequestSpec messagesRequestSpec = queryParamsToFullRequestSpecificationMapper.simpleQueryParamsToFullRequestSpecification(query,
                    splitByComma(streams),
                    splitByComma(streamCategories),
                    timerangeKeyword,
                    splitByComma(fields),
                    sort,
                    sortOrder,
                    from,
                    size);
            return service.executeQuery(messagesRequestSpec, searchUser);
        } catch (IllegalArgumentException | QueryFailedException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    @POST
    @Operation(summary = "Execute aggregation specified by `searchRequestSpec`",
                  operationId = "aggregateSearchRequestSpec")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aggregation executed successfully",
                    content = @Content(schema = @Schema(implementation = TabularResponse.class)))
    })
    @Path("aggregate")
    @NoAuditEvent("Creating audit event manually in method body.")
    public TabularResponse executeQuery(@Parameter(name = "searchRequestSpec") @Valid AggregationRequestSpec aggregationRequestSpec,
                                        @Context SearchUser searchUser) {
        try {
            return service.executeAggregation(aggregationRequestSpec, searchUser);
        } catch (IllegalArgumentException | ValidationException | QueryFailedException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    @GET
    @Operation(summary = "Execute aggregation specified by query parameters", operationId = "aggregateForQueryParameters")
    @Path("aggregate")
    @NoAuditEvent("Creating audit event manually in method body.")
    public TabularResponse executeQuery(@Parameter(name = "query", description = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
                                        @Parameter(name = "streams", description = "Comma separated list of streams to search in (can be empty)", required = true) @QueryParam("streams") Set<String> streams,
                                        @Parameter(name = "stream_categories", description = "Comma separated list of streams categories to search in (can be empty)", required = true) @QueryParam("stream_categories") Set<String> streamCategories,
                                        @Parameter(name = "timerange", description = "Timeframe to search in. See method description.", required = true) @QueryParam("timerange") String timerangeKeyword,
                                        @Parameter(name = "group_by", description = "Group aggregation by fields/limits.", required = true) @QueryParam("groups") List<String> groups,
                                        @Parameter(name = "metrics", description = "Metrics to be used.", required = true) @QueryParam("metrics") List<String> metrics,
                                        @Context SearchUser searchUser) {
        try {
            AggregationRequestSpec aggregationRequestSpec = queryParamsToFullRequestSpecificationMapper.simpleQueryParamsToFullRequestSpecification(
                    query,
                    StringUtils.splitByComma(streams),
                    StringUtils.splitByComma(streamCategories),
                    timerangeKeyword,
                    splitByComma(groups),
                    splitByComma(metrics)
            );
            return service.executeAggregation(aggregationRequestSpec, searchUser);
        } catch (IllegalArgumentException | QueryFailedException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }
}
