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
package org.graylog2.rest.resources.search;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.glassfish.jersey.server.ChunkedOutput;
import org.graylog2.decorators.DecoratorProcessor;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.SearchesConfig;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@RequiresAuthentication
@Api(value = "Legacy/Search/Absolute", description = "Message search")
@Path("/search/universal/absolute")
public class AbsoluteSearchResource extends SearchResource {
    private static final Logger LOG = LoggerFactory.getLogger(AbsoluteSearchResource.class);

    @Inject
    public AbsoluteSearchResource(Searches searches,
                                  ClusterConfigService clusterConfigService,
                                  DecoratorProcessor decoratorProcessor) {
        super(searches, clusterConfigService, decoratorProcessor);
    }

    @GET
    @Timed
    @ApiOperation(value = "Message search with absolute timerange.",
            notes = "Search for messages using an absolute timerange, specified as from/to " +
                    "with format yyyy-MM-ddTHH:mm:ss.SSSZ (e.g. 2014-01-23T15:34:49.000Z) or yyyy-MM-dd HH:mm:ss.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    public SearchResponse searchAbsolute(
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "from", value = "Timerange start. See description for date format", required = true)
            @QueryParam("from") @NotEmpty String from,
            @ApiParam(name = "to", value = "Timerange end. See description for date format", required = true)
            @QueryParam("to") @NotEmpty String to,
            @ApiParam(name = "limit", value = "Maximum number of messages to return.", required = false) @QueryParam("limit") int limit,
            @ApiParam(name = "offset", value = "Offset", required = false) @QueryParam("offset") int offset,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter,
            @ApiParam(name = "fields", value = "Comma separated list of fields to return", required = false) @QueryParam("fields") String fields,
            @ApiParam(name = "sort", value = "Sorting (field:asc / field:desc)", required = false) @QueryParam("sort") String sort,
            @ApiParam(name = "decorate", value = "Run decorators on search result", required = false) @QueryParam("decorate") @DefaultValue("true") boolean decorate) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_ABSOLUTE);

        final Sorting sorting = buildSorting(sort);
        final List<String> fieldList = parseOptionalFields(fields);

        TimeRange timeRange = buildAbsoluteTimeRange(from, to);
        final SearchesConfig searchesConfig = SearchesConfig.builder()
                .query(query)
                .filter(filter)
                .fields(fieldList)
                .range(timeRange)
                .limit(limit)
                .offset(offset)
                .sorting(sorting)
                .build();

        final Optional<String> streamId = Searches.extractStreamId(filter);

        return buildSearchResponse(searches.search(searchesConfig), timeRange, decorate, streamId);
    }

    @GET
    @Timed
    @ApiOperation(value = "Message search with absolute timerange.",
            notes = "Search for messages using an absolute timerange, specified as from/to " +
                    "with format yyyy-MM-ddTHH:mm:ss.SSSZ (e.g. 2014-01-23T15:34:49.000Z) or yyyy-MM-dd HH:mm:ss.")
    @Produces(MoreMediaTypes.TEXT_CSV)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    public ChunkedOutput<ScrollResult.ScrollChunk> searchAbsoluteChunked(
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "from", value = "Timerange start. See description for date format", required = true)
            @QueryParam("from") @NotEmpty String from,
            @ApiParam(name = "to", value = "Timerange end. See description for date format", required = true)
            @QueryParam("to") @NotEmpty String to,
            @ApiParam(name = "limit", value = "Maximum number of messages to return.", required = false) @QueryParam("limit") int limit,
            @ApiParam(name = "offset", value = "Offset", required = false) @QueryParam("offset") int offset,
            @ApiParam(name = "batch_size", value = "Batch size for the backend storage export request.", required = false) @QueryParam("batch_size") @DefaultValue(DEFAULT_SCROLL_BATCH_SIZE) int batchSize,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter,
            @ApiParam(name = "fields", value = "Comma separated list of fields to return", required = true)
            @QueryParam("fields") @NotEmpty String fields) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_ABSOLUTE);

        final List<String> fieldList = parseFields(fields);
        final TimeRange timeRange = buildAbsoluteTimeRange(from, to);

        final ScrollResult scroll = searches
                .scroll(query, timeRange, limit, offset, fieldList, filter, batchSize);
        return buildChunkedOutput(scroll);
    }

    @GET
    @Path("/export")
    @Timed
    @ApiOperation(value = "Export message search with absolute timerange.",
            notes = "Search for messages using an absolute timerange, specified as from/to " +
                    "with format yyyy-MM-ddTHH:mm:ss.SSSZ (e.g. 2014-01-23T15:34:49.000Z) or yyyy-MM-dd HH:mm:ss.")
    @Produces(MoreMediaTypes.TEXT_CSV)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    public Response exportSearchAbsoluteChunked(
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "from", value = "Timerange start. See description for date format", required = true)
            @QueryParam("from") @NotEmpty String from,
            @ApiParam(name = "to", value = "Timerange end. See description for date format", required = true)
            @QueryParam("to") @NotEmpty String to,
            @ApiParam(name = "limit", value = "Maximum number of messages to return.", required = false) @QueryParam("limit") int limit,
            @ApiParam(name = "offset", value = "Offset", required = false) @QueryParam("offset") int offset,
            @ApiParam(name = "batch_size", value = "Batch size for the backend storage export request.", required = false) @QueryParam("batch_size") @DefaultValue(DEFAULT_SCROLL_BATCH_SIZE) int batchSize,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter,
            @ApiParam(name = "fields", value = "Comma separated list of fields to return", required = true)
            @QueryParam("fields") @NotEmpty String fields) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_ABSOLUTE);
        final String filename = "graylog-search-result-absolute-" + from + "-" + to + ".csv";
        return Response
            .ok(searchAbsoluteChunked(query, from, to, limit, offset, batchSize, filter, fields))
            .header("Content-Disposition", "attachment; filename=" + filename)
            .build();
    }

    private TimeRange buildAbsoluteTimeRange(String from, String to) {
        try {
            return restrictTimeRange(AbsoluteRange.create(from, to));
        } catch (InvalidRangeParametersException e) {
            LOG.warn("Invalid timerange parameters provided. Returning HTTP 400.");
            throw new BadRequestException("Invalid timerange parameters provided", e);
        }
    }
}
