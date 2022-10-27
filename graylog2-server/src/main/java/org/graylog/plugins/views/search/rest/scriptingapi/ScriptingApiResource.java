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
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.events.SearchJobExecutionEvent;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;
import org.graylog.plugins.views.search.rest.SearchJobDTO;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.AggregationSpecToPivotMapper;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.SearchRequestSpecToSearchMapper;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.SearchTypeResultToTabularResponseMapper;
import org.graylog.plugins.views.search.rest.scriptingapi.request.SearchRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "ScriptingApi", tags = {CLOUD_VISIBLE})
@Path("/scripting_api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes({MediaType.APPLICATION_JSON})
@RequiresAuthentication
public class ScriptingApiResource extends RestResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(ScriptingApiResource.class);

    private final SearchExecutor searchExecutor;
    private final EventBus serverEventBus;
    private final SearchRequestSpecToSearchMapper searchCreator;
    private final SearchTypeResultToTabularResponseMapper responseCreator;

    @Inject
    public ScriptingApiResource(final SearchExecutor searchExecutor,
                                final EventBus serverEventBus,
                                final SearchRequestSpecToSearchMapper searchCreator,
                                final SearchTypeResultToTabularResponseMapper responseCreator) {
        this.searchExecutor = searchExecutor;
        this.serverEventBus = serverEventBus;
        this.searchCreator = searchCreator;
        this.responseCreator = responseCreator;
    }

    @POST
    @ApiOperation(value = "Execute aggregation specified by `searchRequestSpec`",
                  response = TabularResponse.class)
    @Path("aggregate")
    @NoAuditEvent("Creating audit event manually in method body.")
    @Produces(MediaType.APPLICATION_JSON)
    public Response executeQuery(@ApiParam(name = "searchRequestSpec") @Valid SearchRequestSpec searchRequestSpec,
                                 @Context SearchUser searchUser) {

        //Step 1: map simple request to more complex search
        Search search = searchCreator.apply(searchRequestSpec);

        //Step 2: execute search as we usually do
        final SearchJob searchJob = searchExecutor.execute(search, searchUser, ExecutionState.empty());
        postAuditEvent(searchJob);

        //Step 3: take complex response and try to map it to simpler, tabular form
        final SearchJobDTO searchJobDTO = SearchJobDTO.fromSearchJob(searchJob);
        final QueryResult queryResult = searchJobDTO.results().get(SearchRequestSpecToSearchMapper.QUERY_ID);
        if (queryResult != null) {
            final SearchType.Result aggregationResult = queryResult.searchTypes().get(AggregationSpecToPivotMapper.PIVOT_ID);
            if (aggregationResult instanceof PivotResult pivotResult) {
                final TabularResponse tabularResponse = responseCreator.mapToResponse(searchRequestSpec, pivotResult);
                return Response.ok(tabularResponse).build();
            }
        }

        LOG.warn("Scripting API failed to obtain aggregation for input : " + searchRequestSpec);
        return Response.status(Response.Status.NOT_FOUND)
                .build();
    }

    private void postAuditEvent(SearchJob searchJob) {
        final SearchJobExecutionEvent searchJobExecutionEvent = SearchJobExecutionEvent.create(getCurrentUser(), searchJob, DateTime.now(DateTimeZone.UTC));
        this.serverEventBus.post(searchJobExecutionEvent);
    }
}
