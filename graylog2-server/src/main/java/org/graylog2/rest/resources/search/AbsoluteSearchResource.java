/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.rest.resources.search;

import com.codahale.metrics.annotation.Timed;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Api(value = "Search/Absolute", description = "Message search")
@Path("/search/universal/absolute")
public class AbsoluteSearchResource extends SearchResource {

    private static final Logger LOG = LoggerFactory.getLogger(AbsoluteSearchResource.class);

    @GET @Timed
    @ApiOperation(value = "Message search with absolute timerange.",
            notes = "Search for messages using an absolute timerange, specified as from/to " +
                    "with format yyyy-MM-dd HH-mm-ss.SSS or yyyy-MM-dd HH-mm-ss.")
    @Produces({ MediaType.APPLICATION_JSON, "text/csv" })
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    public SearchResponse searchAbsolute(
            @ApiParam(title = "query", description = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(title = "from", description = "Timerange start. See description for date format", required = true) @QueryParam("from") String from,
            @ApiParam(title = "to", description = "Timerange end. See description for date format", required = true) @QueryParam("to") String to,
            @ApiParam(title = "limit", description = "Maximum number of messages to return.", required = false) @QueryParam("limit") int limit,
            @ApiParam(title = "offset", description = "Offset", required = false) @QueryParam("offset") int offset,
            @ApiParam(title = "filter", description = "Filter", required = false) @QueryParam("filter") String filter) {
        checkQuery(query);

        try {
            SearchResponse searchResponse;

            if (filter == null) {
                searchResponse = buildSearchResponse(
                        core.getIndexer().searches().search(query, buildAbsoluteTimeRange(from, to), limit, offset)
                );
            } else {
                searchResponse = buildSearchResponse(
                        core.getIndexer().searches().search(query, filter, buildAbsoluteTimeRange(from, to), limit, offset)
                );
            }

            return searchResponse;
        } catch (IndexHelper.InvalidRangeFormatException e) {
            LOG.warn("Invalid timerange parameters provided. Returning HTTP 400.", e);
            throw new WebApplicationException(400);
        }
    }

    @GET @Path("/terms") @Timed
    @ApiOperation(value = "Most common field terms of a query using an absolute timerange.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public String termsAbsolute(
            @ApiParam(title = "field", description = "Message field of to return terms of", required = true) @QueryParam("field") String field,
            @ApiParam(title = "query", description = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(title = "size", description = "Maximum number of terms to return", required = false) @QueryParam("size") int size,
            @ApiParam(title = "from", description = "Timerange start. See search method description for date format", required = true) @QueryParam("from") String from,
            @ApiParam(title = "to", description = "Timerange end. See search method description for date format", required = true) @QueryParam("to") String to,
            @ApiParam(title = "filter", description = "Filter", required = false) @QueryParam("filter") String filter) {
        checkQueryAndField(query, field);

        try {
            return json(buildTermsResult(
                    core.getIndexer().searches().terms(field, size, query, filter, buildAbsoluteTimeRange(from, to))
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
            @ApiParam(title = "field", description = "Message field of numeric type to return statistics for", required = true) @QueryParam("field") String field,
            @ApiParam(title = "query", description = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(title = "from", description = "Timerange start. See search method description for date format", required = true) @QueryParam("from") String from,
            @ApiParam(title = "to", description = "Timerange end. See search method description for date format", required = true) @QueryParam("to") String to,
            @ApiParam(title = "filter", description = "Filter", required = false) @QueryParam("filter") String filter) {
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
            @ApiParam(title = "query", description = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(title = "interval", description = "Histogram interval / bucket size. (year, quarter, month, week, day, hour or minute)", required = true) @QueryParam("interval") String interval,
            @ApiParam(title = "from", description = "Timerange start. See search method description for date format", required = true) @QueryParam("from") String from,
            @ApiParam(title = "to", description = "Timerange end. See search method description for date format", required = true) @QueryParam("to") String to,
            @ApiParam(title = "filter", description = "Filter", required = false) @QueryParam("filter") String filter) {
        checkQueryAndInterval(query, interval);
        interval = interval.toUpperCase();
        validateInterval(interval);

        try {
            return json(buildHistogramResult(
                    core.getIndexer().searches().histogram(
                            query,
                            Indexer.DateHistogramInterval.valueOf(interval),
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
            @ApiParam(title = "query", description = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(title = "field", description = "Field of whose values to get the histogram of", required = true) @QueryParam("field") String field,
            @ApiParam(title = "interval", description = "Histogram interval / bucket size. (year, quarter, month, week, day, hour or minute)", required = true) @QueryParam("interval") String interval,
            @ApiParam(title = "from", description = "Timerange start. See search method description for date format", required = true) @QueryParam("from") String from,
            @ApiParam(title = "to", description = "Timerange end. See search method description for date format", required = true) @QueryParam("to") String to,
            @ApiParam(title = "filter", description = "Filter", required = false) @QueryParam("filter") String filter) {
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
