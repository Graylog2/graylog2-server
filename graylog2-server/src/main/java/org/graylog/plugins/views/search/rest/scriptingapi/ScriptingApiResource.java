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

import com.google.common.eventbus.EventBus;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.events.SearchJobExecutionEvent;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.AggregationTabularResponseCreator;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.MessagesTabularResponseCreator;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.QueryFailedException;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.QueryParamsToFullRequestSpecificationMapper;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.SearchRequestSpecToSearchMapper;
import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.MessagesRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.utilities.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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

import java.util.List;
import java.util.Set;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;
import static org.graylog2.shared.utilities.StringUtils.splitByComma;

@Api(value = "Search/Simple", description = "Simple search API for aggregating and messages retrieval", tags = {CLOUD_VISIBLE})
@Path("/search")
@Consumes({MediaType.APPLICATION_JSON})
@RequiresAuthentication
public class ScriptingApiResource extends RestResource implements PluginRestResource {

    private final SearchExecutor searchExecutor;
    private final EventBus serverEventBus;
    private final SearchRequestSpecToSearchMapper searchCreator;
    private final AggregationTabularResponseCreator aggregationTabularResponseCreator;
    private final MessagesTabularResponseCreator messagesTabularResponseCreator;
    private final QueryParamsToFullRequestSpecificationMapper queryParamsToFullRequestSpecificationMapper;

    @Inject
    public ScriptingApiResource(final SearchExecutor searchExecutor,
                                final EventBus serverEventBus,
                                final SearchRequestSpecToSearchMapper searchCreator,
                                final AggregationTabularResponseCreator aggregationTabularResponseCreator,
                                final MessagesTabularResponseCreator messagesTabularResponseCreator,
                                final QueryParamsToFullRequestSpecificationMapper queryParamsToFullRequestSpecificationMapper) {
        this.searchExecutor = searchExecutor;
        this.serverEventBus = serverEventBus;
        this.searchCreator = searchCreator;
        this.aggregationTabularResponseCreator = aggregationTabularResponseCreator;
        this.messagesTabularResponseCreator = messagesTabularResponseCreator;
        this.queryParamsToFullRequestSpecificationMapper = queryParamsToFullRequestSpecificationMapper;
    }

    @POST
    @ApiOperation(value = "Execute query specified by `queryRequestSpec`",
                  nickname = "messagesByQueryRequestSpec",
                  response = TabularResponse.class)
    @Path("messages")
    @NoAuditEvent("Creating audit event manually in method body.")
    public TabularResponse executeQuery(@ApiParam(name = "queryRequestSpec") @Valid MessagesRequestSpec messagesRequestSpec,
                                        @Context SearchUser searchUser) {

        try {
            //Step 1: map simple request to more complex search
            Search search = searchCreator.mapToSearch(messagesRequestSpec, searchUser);

            //Step 2: execute search as we usually do
            final SearchJob searchJob = searchExecutor.execute(search, searchUser, ExecutionState.empty());
            postAuditEvent(searchJob);

            //Step 3: take complex response and try to map it to simpler, tabular form
            return messagesTabularResponseCreator.mapToResponse(messagesRequestSpec, searchJob, searchUser, getSubject());

        } catch (IllegalArgumentException | ValidationException | QueryFailedException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    @GET
    @ApiOperation(value = "Execute query specified by query parameters", nickname = "messagesByQueryParameters")
    @Path("messages")
    @NoAuditEvent("Creating audit event manually in method body.")
    public TabularResponse executeQuery(@ApiParam(name = "query") @QueryParam("query") String query,
                                        @ApiParam(name = "streams") @QueryParam("streams") Set<String> streams,
                                        @ApiParam(name = "timerange") @QueryParam("timerange") String timerangeKeyword,
                                        @ApiParam(name = "fields") @QueryParam("fields") List<String> fields,
                                        @ApiParam(name = "sort") @QueryParam("sort") String sort,
                                        @ApiParam(name = "sort") @QueryParam("sortOrder") SortSpec.Direction sortOrder,
                                        @ApiParam(name = "from") @QueryParam("from") int from,
                                        @ApiParam(name = "size") @QueryParam("size") int size,
                                        @Context SearchUser searchUser) {

        try {
            MessagesRequestSpec messagesRequestSpec = queryParamsToFullRequestSpecificationMapper.simpleQueryParamsToFullRequestSpecification(query,
                    splitByComma(streams),
                    timerangeKeyword,
                    splitByComma(fields),
                    sort,
                    sortOrder,
                    from,
                    size);
            return executeQuery(messagesRequestSpec, searchUser);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    @POST
    @ApiOperation(value = "Execute aggregation specified by `searchRequestSpec`",
                  nickname = "aggregateSearchRequestSpec",
                  response = TabularResponse.class)
    @Path("aggregate")
    @NoAuditEvent("Creating audit event manually in method body.")
    public TabularResponse executeQuery(@ApiParam(name = "searchRequestSpec") @Valid AggregationRequestSpec aggregationRequestSpec,
                                        @Context SearchUser searchUser) {
        try {
            //Step 1: map simple request to more complex search
            Search search = searchCreator.mapToSearch(aggregationRequestSpec, searchUser);

            //Step 2: execute search as we usually do
            final SearchJob searchJob = searchExecutor.execute(search, searchUser, ExecutionState.empty());
            postAuditEvent(searchJob);

            //Step 3: take complex response and try to map it to simpler, tabular form
            return aggregationTabularResponseCreator.mapToResponse(aggregationRequestSpec, searchJob, searchUser);
        } catch (IllegalArgumentException | ValidationException | QueryFailedException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    @GET
    @ApiOperation(value = "Execute aggregation specified by query parameters", nickname = "aggregateForQueryParameters")
    @Path("aggregate")
    @NoAuditEvent("Creating audit event manually in method body.")
    public TabularResponse executeQuery(@ApiParam(name = "query") @QueryParam("query") String query,
                                        @ApiParam(name = "streams") @QueryParam("streams") Set<String> streams,
                                        @ApiParam(name = "timerange") @QueryParam("timerange") String timerangeKeyword,
                                        @ApiParam(name = "groups") @QueryParam("groups") List<String> groups,
                                        @ApiParam(name = "metrics") @QueryParam("metrics") List<String> metrics,
                                        @Context SearchUser searchUser) {
        try {
            AggregationRequestSpec aggregationRequestSpec = queryParamsToFullRequestSpecificationMapper.simpleQueryParamsToFullRequestSpecification(
                    query,
                    StringUtils.splitByComma(streams),
                    timerangeKeyword,
                    splitByComma(groups),
                    splitByComma(metrics)
            );
            return executeQuery(aggregationRequestSpec, searchUser);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    private void postAuditEvent(SearchJob searchJob) {
        final SearchJobExecutionEvent searchJobExecutionEvent = SearchJobExecutionEvent.create(getCurrentUser(), searchJob, DateTime.now(DateTimeZone.UTC));
        this.serverEventBus.post(searchJobExecutionEvent);
    }
}
