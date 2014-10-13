/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.search;

import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.glassfish.jersey.server.ChunkedOutput;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.SearchesConfig;
import org.graylog2.indexer.searches.SearchesConfigBuilder;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import com.wordnik.swagger.annotations.*;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.graylog2.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Api(value = "Search/Absolute", description = "Message search")
@Path("/search/universal/absolute")
public class AbsoluteSearchResource extends SearchResource {

    private static final Logger LOG = LoggerFactory.getLogger(AbsoluteSearchResource.class);

    @Inject
    public AbsoluteSearchResource(Searches searches) {
        super(searches);
    }

    @GET @Timed
    @ApiOperation(value = "Message search with absolute timerange.",
            notes = "Search for messages using an absolute timerange, specified as from/to " +
                    "with format yyyy-MM-ddTHH:mm:ss.SSSZ (e.g. 2014-01-23T15:34:49.000Z) or yyyy-MM-dd HH:mm:ss.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    public SearchResponse searchAbsolute(
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(name = "from", value = "Timerange start. See description for date format", required = true) @QueryParam("from") String from,
            @ApiParam(name = "to", value = "Timerange end. See description for date format", required = true) @QueryParam("to") String to,
            @ApiParam(name = "limit", value = "Maximum number of messages to return.", required = false) @QueryParam("limit") int limit,
            @ApiParam(name = "offset", value = "Offset", required = false) @QueryParam("offset") int offset,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter,
            @ApiParam(name = "fields", value = "Comma separated list of fields to return", required = false) @QueryParam("fields") String fields,
            @ApiParam(name = "sort", value = "Sorting (field:asc / field:desc)", required = false) @QueryParam("sort") String sort) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_ABSOLUTE);

        checkQuery(query);

        Sorting sorting = buildSorting(sort);

        final List<String> fieldList = parseOptionalFields(fields);
        TimeRange timeRange = buildAbsoluteTimeRange(from, to);
        final SearchesConfig searchesConfig = SearchesConfigBuilder.newConfig()
                .setQuery(query)
                .setFilter(filter)
                .setFields(fieldList)
                .setRange(timeRange)
                .setLimit(limit)
                .setOffset(offset)
                .setSorting(sorting)
                .build();

        try {
            return buildSearchResponse(searches.search(searchesConfig), timeRange);
        } catch (IndexHelper.InvalidRangeFormatException e) {
            LOG.warn("Invalid timerange parameters provided. Returning HTTP 400.", e);
            throw new WebApplicationException(400);
        } catch (SearchPhaseExecutionException e) {
            throw createRequestExceptionForParseFailure(query, e);
        }
    }

    @GET @Timed
    @ApiOperation(value = "Message search with absolute timerange.",
                  notes = "Search for messages using an absolute timerange, specified as from/to " +
                          "with format yyyy-MM-ddTHH:mm:ss.SSSZ (e.g. 2014-01-23T15:34:49.000Z) or yyyy-MM-dd HH:mm:ss.")
    @Produces("text/csv")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    public ChunkedOutput<ScrollResult.ScrollChunk> searchAbsoluteChunked(
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(name = "from", value = "Timerange start. See description for date format", required = true) @QueryParam("from") String from,
            @ApiParam(name = "to", value = "Timerange end. See description for date format", required = true) @QueryParam("to") String to,
            @ApiParam(name = "limit", value = "Maximum number of messages to return.", required = false) @QueryParam("limit") int limit,
            @ApiParam(name = "offset", value = "Offset", required = false) @QueryParam("offset") int offset,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter,
            @ApiParam(name = "fields", value = "Comma separated list of fields to return", required = true) @QueryParam("fields") String fields) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_ABSOLUTE);

        checkQuery(query);
        final List<String> fieldList = parseFields(fields);
        final TimeRange timeRange = buildAbsoluteTimeRange(from, to);

        try {
            final ScrollResult scroll = searches
                    .scroll(query, timeRange, limit, offset, fieldList, filter);
            final ChunkedOutput<ScrollResult.ScrollChunk> output = new ChunkedOutput<>(ScrollResult.ScrollChunk.class);

            LOG.debug("[{}] Scroll result contains a total of {} messages", scroll.getQueryHash(), scroll.totalHits());
            Runnable scrollIterationAction = createScrollChunkProducer(scroll, output, limit);
            // TODO use a shared executor for async responses here instead of a single thread that's not limited
            new Thread(scrollIterationAction).start();
            return output;
        } catch (SearchPhaseExecutionException e) {
            throw createRequestExceptionForParseFailure(query, e);
        } catch (IndexHelper.InvalidRangeFormatException e) {
            throw new BadRequestException(e);
        }
    }

    @GET @Path("/terms") @Timed
    @ApiOperation(value = "Most common field terms of a query using an absolute timerange.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public String termsAbsolute(
            @ApiParam(name = "field", value = "Message field of to return terms of", required = true) @QueryParam("field") String field,
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(name = "size", value = "Maximum number of terms to return", required = false) @QueryParam("size") int size,
            @ApiParam(name = "from", value = "Timerange start. See search method description for date format", required = true) @QueryParam("from") String from,
            @ApiParam(name = "to", value = "Timerange end. See search method description for date format", required = true) @QueryParam("to") String to,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_ABSOLUTE);

        checkQueryAndField(query, field);

        try {
            return json(buildTermsResult(
                    searches.terms(field, size, query, filter, buildAbsoluteTimeRange(from, to))
            ));
        } catch (IndexHelper.InvalidRangeFormatException e) {
            LOG.warn("Invalid timerange parameters provided. Returning HTTP 400.", e);
            throw new WebApplicationException(400);
        }
    }

    @GET @Path("/termsstats") @Timed
    @ApiOperation(value = "Ordered field terms of a query computed on another field using an absolute timerange.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public String termsStatsAbsolute(
            @ApiParam(name = "key_field", value = "Message field of to return terms of", required = true) @QueryParam("key_field") String keyField,
            @ApiParam(name = "value_field", value = "Value field used for computation", required = true) @QueryParam("value_field") String valueField,
            @ApiParam(name = "order", value = "What to order on (Allowed values: TERM, REVERSE_TERM, COUNT, REVERSE_COUNT, TOTAL, REVERSE_TOTAL, MIN, REVERSE_MIN, MAX, REVERSE_MAX, MEAN, REVERSE_MEAN)", required = true) @QueryParam("order") String order,
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(name = "size", value = "Maximum number of terms to return", required = false) @QueryParam("size") int size,
            @ApiParam(name = "from", value = "Timerange start. See search method description for date format", required = true) @QueryParam("from") String from,
            @ApiParam(name = "to", value = "Timerange end. See search method description for date format", required = true) @QueryParam("to") String to,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter) throws IndexHelper.InvalidRangeFormatException {
        checkSearchPermission(filter, RestPermissions.SEARCHES_ABSOLUTE);

        checkTermsStatsFields(keyField, valueField, order);
        checkQuery(query);

        try {
            return json(buildTermsStatsResult(
                    searches.termsStats(keyField,
                                        valueField,
                                        Searches.TermsStatsOrder.valueOf(order.toUpperCase()),
                                        size,
                                        query,
                                        filter,
                                        buildAbsoluteTimeRange(from, to))
            ));
        } catch (IndexHelper.InvalidRangeFormatException e) {
            LOG.warn("Invalid timerange parameters provided. Returning HTTP 400.", e);
            throw new WebApplicationException(400);
        }
    }

    @GET @Path("/stats") @Timed
    @ApiOperation(value = "Field statistics for a query using an absolute timerange.",
            notes = "Returns statistics like min/max or standard deviation of numeric fields " +
                    "over the whole query result set.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided."),
            @ApiResponse(code = 400, message = "Field is not of numeric type.")
    })
    public String statsAbsolute(
            @ApiParam(name = "field", value = "Message field of numeric type to return statistics for", required = true) @QueryParam("field") String field,
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(name = "from", value = "Timerange start. See search method description for date format", required = true) @QueryParam("from") String from,
            @ApiParam(name = "to", value = "Timerange end. See search method description for date format", required = true) @QueryParam("to") String to,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_ABSOLUTE);

        checkQueryAndField(query, field);

        try {
            return json(buildFieldStatsResult(
                    fieldStats(field, query, filter, buildAbsoluteTimeRange(from, to))
            ));
        } catch (IndexHelper.InvalidRangeFormatException e) {
            LOG.warn("Invalid timerange parameters provided. Returning HTTP 400.", e);
            throw new WebApplicationException(400);
        }
    }

    @GET @Path("/histogram") @Timed
    @ApiOperation(value = "Datetime histogram of a query using an absolute timerange.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided."),
            @ApiResponse(code = 400, message = "Invalid interval provided.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public String histogramAbsolute(
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(name = "interval", value = "Histogram interval / bucket size. (year, quarter, month, week, day, hour or minute)", required = true) @QueryParam("interval") String interval,
            @ApiParam(name = "from", value = "Timerange start. See search method description for date format", required = true) @QueryParam("from") String from,
            @ApiParam(name = "to", value = "Timerange end. See search method description for date format", required = true) @QueryParam("to") String to,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_ABSOLUTE);

        checkQueryAndInterval(query, interval);
        interval = interval.toUpperCase();
        validateInterval(interval);

        try {
            return json(buildHistogramResult(
                    searches.histogram(
                            query,
                            Searches.DateHistogramInterval.valueOf(interval),
                            filter,
                            buildAbsoluteTimeRange(from, to)
                    )
            ));
        } catch (IndexHelper.InvalidRangeFormatException e) {
            LOG.warn("Invalid timerange parameters provided. Returning HTTP 400.", e);
            throw new WebApplicationException(400);
        }
    }

    @GET @Path("/fieldhistogram") @Timed
    @ApiOperation(value = "Field value histogram of a query using an absolute timerange.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided."),
            @ApiResponse(code = 400, message = "Invalid interval provided."),
            @ApiResponse(code = 400, message = "Field is not of numeric type.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public String fieldHistogramAbsolute(
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(name = "field", value = "Field of whose values to get the histogram of", required = true) @QueryParam("field") String field,
            @ApiParam(name = "interval", value = "Histogram interval / bucket size. (year, quarter, month, week, day, hour or minute)", required = true) @QueryParam("interval") String interval,
            @ApiParam(name = "from", value = "Timerange start. See search method description for date format", required = true) @QueryParam("from") String from,
            @ApiParam(name = "to", value = "Timerange end. See search method description for date format", required = true) @QueryParam("to") String to,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_ABSOLUTE);

        checkQueryAndInterval(query, interval);
        interval = interval.toUpperCase();
        validateInterval(interval);
        checkStringSet(field);

        try {
            return json(buildHistogramResult(fieldHistogram(field, query, interval, filter, buildAbsoluteTimeRange(from, to))));
        } catch (IndexHelper.InvalidRangeFormatException e) {
            LOG.warn("Invalid timerange parameters provided. Returning HTTP 400.", e);
            throw new WebApplicationException(400);
        }

    }

    private TimeRange buildAbsoluteTimeRange(String from, String to) {
        try {
            return new AbsoluteRange(from, to);
        } catch (InvalidRangeParametersException e) {
            LOG.warn("Invalid timerange parameters provided. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
    }

}
