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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.glassfish.jersey.server.ChunkedOutput;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog2.decorators.DecoratorProcessor;
import org.graylog2.indexer.results.ChunkedResult;
import org.graylog2.indexer.results.ResultChunk;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.graylog2.rest.RestTools.respondWithFile;

@RequiresAuthentication
@PublicCloudAPI
@Tag(name = "Legacy/Search/Absolute", description = "Message search")
@Path("/search/universal/absolute")
public class AbsoluteSearchResource extends SearchResource {
    private static final Logger LOG = LoggerFactory.getLogger(AbsoluteSearchResource.class);

    @Inject
    public AbsoluteSearchResource(Searches searches,
                                  SearchExecutor searchExecutor,
                                  ClusterConfigService clusterConfigService,
                                  DecoratorProcessor decoratorProcessor) {
        super(searches, clusterConfigService, decoratorProcessor, searchExecutor);
    }

    @GET
    @Timed
    @Operation(summary = "Message search with absolute timerange.",
                  description = "Search for messages using an absolute timerange, specified as from/to " +
                          "with format yyyy-MM-ddTHH:mm:ss.SSSZ (e.g. 2014-01-23T15:34:49.000Z) or yyyy-MM-dd HH:mm:ss.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "Invalid timerange parameters provided.")
    })
    public SearchResponse searchAbsolute(
            @Parameter(name = "query", description = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @Parameter(name = "from", description = "Timerange start. See description for date format", required = true)
            @QueryParam("from") @NotEmpty String from,
            @Parameter(name = "to", description = "Timerange end. See description for date format", required = true)
            @QueryParam("to") @NotEmpty String to,
            @Parameter(name = "limit", description = "Maximum number of messages to return.") @QueryParam("limit") int limit,
            @Parameter(name = "offset", description = "Offset") @QueryParam("offset") int offset,
            @Parameter(name = "filter", description = "Filter") @QueryParam("filter") String filter,
            @Parameter(name = "streams", description = "Comma separated list of stream IDs to search in") @QueryParam("streams")  String streams,
            @Parameter(name = "fields", description = "Comma separated list of fields to return") @QueryParam("fields") String fields,
            @Parameter(name = "sort", description = "Sorting (field:asc / field:desc)") @QueryParam("sort") String sort,
            @Parameter(name = "decorate", description = "Run decorators on search result") @QueryParam("decorate") @DefaultValue("true") boolean decorate,
            @Context SearchUser searchUser) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_ABSOLUTE);

        final Sort sorting = buildSortOrder(sort);
        final List<String> fieldList = parseOptionalFields(fields);

        final TimeRange timeRange = buildAbsoluteTimeRange(from, to);
        final var parsedStreams = parseStreams(streams);

        return search(query, limit, offset, filter, parsedStreams, decorate, searchUser, fieldList, sorting, timeRange);
    }

    @GET
    @Timed
    @Operation(summary = "Message search with absolute timerange.",
                  description = "Search for messages using an absolute timerange, specified as from/to " +
                          "with format yyyy-MM-ddTHH:mm:ss.SSSZ (e.g. 2014-01-23T15:34:49.000Z) or yyyy-MM-dd HH:mm:ss.")
    @Produces(MoreMediaTypes.TEXT_CSV)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = MoreMediaTypes.TEXT_CSV,
                            schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "Invalid timerange parameters provided.")
    })
    public ChunkedOutput<ResultChunk> searchAbsoluteChunked(
            @Parameter(name = "query", description = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @Parameter(name = "from", description = "Timerange start. See description for date format", required = true)
            @QueryParam("from") @NotEmpty String from,
            @Parameter(name = "to", description = "Timerange end. See description for date format", required = true)
            @QueryParam("to") @NotEmpty String to,
            @Parameter(name = "limit", description = "Maximum number of messages to return.", required = false) @QueryParam("limit") int limit,
            @Parameter(name = "offset", description = "Offset", required = false) @QueryParam("offset") int offset,
            @Parameter(name = "batch_size", description = "Batch size for the backend storage export request.", required = false) @QueryParam("batch_size") @DefaultValue(DEFAULT_SCROLL_BATCH_SIZE) int batchSize,
            @Parameter(name = "filter", description = "Filter", required = false) @QueryParam("filter") String filter,
            @Parameter(name = "streams", description = "Comma separated list of streams to search in") @QueryParam("streams")  String streams,
            @Parameter(name = "fields", description = "Comma separated list of fields to return", required = true)
            @QueryParam("fields") @NotEmpty String fields) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_ABSOLUTE);

        final List<String> fieldList = parseFields(fields);
        final TimeRange timeRange = buildAbsoluteTimeRange(from, to);
        final var parsedStreams = parseStreams(streams);

        final ChunkedResult scroll = searches
                .scroll(query, timeRange, limit, offset, fieldList, filter, parsedStreams, batchSize);
        return buildChunkedOutput(scroll);
    }

    @GET
    @Path("/export")
    @Timed
    @Operation(summary = "Export message search with absolute timerange.",
                  description = "Search for messages using an absolute timerange, specified as from/to " +
                          "with format yyyy-MM-ddTHH:mm:ss.SSSZ (e.g. 2014-01-23T15:34:49.000Z) or yyyy-MM-dd HH:mm:ss.")
    @Produces(MoreMediaTypes.TEXT_CSV)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = MoreMediaTypes.TEXT_CSV,
                            schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "Invalid timerange parameters provided.")
    })
    public Response exportSearchAbsoluteChunked(
            @Parameter(name = "query", description = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @Parameter(name = "from", description = "Timerange start. See description for date format", required = true)
            @QueryParam("from") @NotEmpty String from,
            @Parameter(name = "to", description = "Timerange end. See description for date format", required = true)
            @QueryParam("to") @NotEmpty String to,
            @Parameter(name = "limit", description = "Maximum number of messages to return.", required = false) @QueryParam("limit") int limit,
            @Parameter(name = "offset", description = "Offset", required = false) @QueryParam("offset") int offset,
            @Parameter(name = "batch_size", description = "Batch size for the backend storage export request.", required = false) @QueryParam("batch_size") @DefaultValue(DEFAULT_SCROLL_BATCH_SIZE) int batchSize,
            @Parameter(name = "filter", description = "Filter", required = false) @QueryParam("filter") String filter,
            @Parameter(name = "streams", description = "Comma separated list of streams to search in") @QueryParam("streams")  String streams,
            @Parameter(name = "fields", description = "Comma separated list of fields to return", required = true)
            @QueryParam("fields") @NotEmpty String fields) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_ABSOLUTE);
        final String filename = "graylog-search-result-absolute-" + from + "-" + to + ".csv";
        return respondWithFile(filename, searchAbsoluteChunked(query, from, to, limit, offset, batchSize, filter, streams, fields))
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
