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
import de.vandermeer.asciitable.AsciiTable;
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
import org.graylog.plugins.views.search.rest.scriptingapi.request.Grouping;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Metric;
import org.graylog.plugins.views.search.rest.scriptingapi.request.SearchRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseSchemaEntry;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "ScriptingApi", tags = {CLOUD_VISIBLE})
@Path("/search")
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
    public TabularResponse executeQuery(@ApiParam(name = "searchRequestSpec") @Valid SearchRequestSpec searchRequestSpec,
                                        @Context SearchUser searchUser) {

        //Step 1: map simple request to more complex search
        Search search = searchCreator.mapToSearch(searchRequestSpec, searchUser);

        //Step 2: execute search as we usually do
        final SearchJob searchJob = searchExecutor.execute(search, searchUser, ExecutionState.empty());
        postAuditEvent(searchJob);

        //Step 3: take complex response and try to map it to simpler, tabular form
        final SearchJobDTO searchJobDTO = SearchJobDTO.fromSearchJob(searchJob);
        final QueryResult queryResult = searchJobDTO.results().get(SearchRequestSpecToSearchMapper.QUERY_ID);
        if (queryResult != null) {
            final SearchType.Result aggregationResult = queryResult.searchTypes().get(AggregationSpecToPivotMapper.PIVOT_ID);
            if (aggregationResult instanceof PivotResult pivotResult) {
                return responseCreator.mapToResponse(searchRequestSpec, pivotResult);
            }
        }

        LOG.warn("Scripting API failed to obtain aggregation for input : " + searchRequestSpec);
        throw new NotFoundException("Scripting API failed to obtain aggregation for input : " + searchRequestSpec);
    }

    @POST
    @ApiOperation(value = "Execute aggregation specified by `searchRequestSpec`",
                  response = TabularResponse.class)
    @Path("aggregate")
    @NoAuditEvent("Creating audit event manually in method body.")
    @Produces(MediaType.TEXT_PLAIN)
    public String executeQueryAsciiOutput(@ApiParam(name = "searchRequestSpec") @Valid SearchRequestSpec searchRequestSpec,
                                          @Context SearchUser searchUser) {
        final TabularResponse response = executeQuery(searchRequestSpec, searchUser);
        AsciiTable at = new AsciiTable();
        at.getContext().setWidth(response.schema().size() * 25);
        at.addRule();
        at.addRow(response.schema().stream().map(ResponseSchemaEntry::name).collect(Collectors.toList()));
        at.addRow(response.schema().stream().map(f -> f.field() != null ? f.field() : "").collect(Collectors.toList()));
        at.addRow(response.schema().stream().map(f -> f.type() != null ? f.type() : "").collect(Collectors.toList()));
        at.addRule();
        response.datarows().forEach(at::addRow);
        at.addRule();
        return at.render();
    }

    @GET
    @ApiOperation(value = "Execute aggregation specified by query parameters",
                  response = TabularResponse.class)
    @Path("aggregate")
    @NoAuditEvent("Creating audit event manually in method body.")
    @Produces(MediaType.APPLICATION_JSON)
    public TabularResponse executeQuery(@QueryParam("query") String query,
                                        @QueryParam("streams") Set<String> streams,
                                        @QueryParam("timerange") String timerangeKeyword,
                                        @QueryParam("groups") List<String> groups,
                                        @QueryParam("metrics") List<String> metrics,
                                        @Context SearchUser searchUser) {
        SearchRequestSpec searchRequestSpec = simpleQueryParamsToFullRequestSpecification(query, streams, timerangeKeyword, groups, metrics);
        return executeQuery(searchRequestSpec, searchUser);
    }

    @GET
    @ApiOperation(value = "Execute aggregation specified by query parameters",
                  response = TabularResponse.class)
    @Path("aggregate")
    @NoAuditEvent("Creating audit event manually in method body.")
    @Produces(MediaType.TEXT_PLAIN)
    public String executeQueryAsciiOutput(@QueryParam("query") String query,
                                          @QueryParam("streams") Set<String> streams,
                                          @QueryParam("timerange") String timerangeKeyword,
                                          @QueryParam("groups") List<String> groups,
                                          @QueryParam("metrics") List<String> metrics,
                                          @Context SearchUser searchUser) {
        SearchRequestSpec searchRequestSpec = simpleQueryParamsToFullRequestSpecification(query, streams, timerangeKeyword, groups, metrics);
        return executeQueryAsciiOutput(searchRequestSpec, searchUser);
    }

    private SearchRequestSpec simpleQueryParamsToFullRequestSpecification(String query, Set<String> streams, String timerangeKeyword, List<String> groups, List<String> metrics) {
        if (groups == null || groups.isEmpty()) {
            throw new BadRequestException("At least one grouping has to be provided!");
        }
        if (metrics == null || metrics.isEmpty()) {
            metrics = List.of("count:");
        }
        if (!metrics.stream().allMatch(m -> m.contains(":"))) {
            throw new BadRequestException("All metrics need to be defined as \"function\":\"field_name\"");
        }

        return new SearchRequestSpec(
                query,
                streams,
                parseTimeRange(timerangeKeyword),
                groups.stream().map(Grouping::new).collect(Collectors.toList()),
                metrics.stream().map(this::parseMetric).collect(Collectors.toList()),
                false
        );
    }

    private TimeRange parseTimeRange(String timerangeKeyword) {
        return timerangeKeyword != null ? KeywordRange.create(timerangeKeyword, "UTC") : null;
    }

    private Metric parseMetric(String metric) {
        final String[] split = metric.split(":");
        final String fieldName = split.length > 1 ? split[1] : null;
        final String functionName = split[0];
        return new Metric(fieldName, functionName, null);
    }

    private void postAuditEvent(SearchJob searchJob) {
        final SearchJobExecutionEvent searchJobExecutionEvent = SearchJobExecutionEvent.create(getCurrentUser(), searchJob, DateTime.now(DateTimeZone.UTC));
        this.serverEventBus.post(searchJobExecutionEvent);
    }
}
