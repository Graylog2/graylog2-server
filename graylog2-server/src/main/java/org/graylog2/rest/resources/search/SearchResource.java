/**
 * Copyright 2013 Lennart Koopmann <lennart@socketfeed.com>
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
import com.google.common.collect.Maps;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.results.DateHistogramResult;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.results.TermsResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Api(value = "Search", description = "Message search")
@Path("/search")
public class SearchResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SearchResource.class);

    /*
     * Relative timerange.
     */

    @GET @Path("/universal/relative") @Timed
    @ApiOperation(value = "Message search with relative timerange.",
                  notes = "Search for messages in a relative timerange, specified as seconds from now. " +
                          "Example: 300 means search from 5 minutes ago to now.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    public String searchRelative(
            @ApiParam(title = "query", description = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(title = "range", description = "Relative timeframe to search in. See method description.", required = true) @QueryParam("range") int range,
            @ApiParam(title = "limit", description = "Maximum number of messages to return.", required = false) @QueryParam("limit") int limit) {
        checkQuery(query);

        return json(buildSearchResult(
                core.getIndexer().searches().search(query, buildRelativeTimeRange(range), limit)
        ));
    }

    @GET @Path("/universal/relative/terms") @Timed
    @ApiOperation(value = "Most common field terms of a query using a relative timerange.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public String termsRelative(
            @ApiParam(title = "field", description = "Message field of to return terms of", required = true) @QueryParam("field") String field,
            @ApiParam(title = "query", description = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(title = "size", description = "Maximum number of terms to return", required = false) @QueryParam("size") int size,
            @ApiParam(title = "range", description = "Relative timeframe to search in. See search method description.", required = true) @QueryParam("range") int range) {
        checkQueryAndField(query, field);

        return json(buildTermsResult(
                core.getIndexer().searches().terms(field, size, query, buildRelativeTimeRange(range))
        ));
    }

    @GET @Path("/universal/relative/stats") @Timed
    @ApiOperation(value = "Field statistics for a query using a relative timerange.",
                  notes = "Returns statistics like min/max or standard deviation of numeric fields " +
                          "over the whole query result set.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided."),
            @ApiResponse(code = 400, message = "Field is not of numeric type.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public String statsRelative(
            @ApiParam(title = "field", description = "Message field of numeric type to return statistics for", required = true) @QueryParam("field") String field,
            @ApiParam(title = "query", description = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(title = "range", description = "Relative timeframe to search in. See search method description.", required = true) @QueryParam("range") int range) {
        checkQueryAndField(query, field);

        return json(buildFieldStatsResult(
                fieldStats(field, query, buildRelativeTimeRange(range))
        ));
    }

    @GET @Path("/universal/relative/histogram") @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String histogramRelative(@QueryParam("query") String query, @QueryParam("interval") String interval, @QueryParam("range") int range) {
        interval = interval.toUpperCase();
        checkQueryAndInterval(query, interval);
        validateInterval(interval);

        return json(buildHistogramResult(
                core.getIndexer().searches().histogram(
                        query,
                        Indexer.DateHistogramInterval.valueOf(interval),
                        buildRelativeTimeRange(range)
                )
        ));
    }

    /*
     * Absolute timerange.
     */

    @GET @Path("/universal/absolute") @Timed
    @ApiOperation(value = "Message search with absolute timerange.",
                  notes = "Search for messages using an absolute timerange, specified as from/to " +
                          "with format yyyy-MM-dd HH-mm-ss.SSS or yyyy-MM-dd HH-mm-ss.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    public String searchAbsolute(
            @ApiParam(title = "query", description = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(title = "from", description = "Timerange start. See description for date format", required = true) @QueryParam("from") String from,
            @ApiParam(title = "to", description = "Timerange end. See description for date format", required = true) @QueryParam("to") String to,
            @ApiParam(title = "limit", description = "Maximum number of messages to return.", required = false) @QueryParam("limit") int limit) {
        checkQuery(query);

        return json(buildSearchResult(
                core.getIndexer().searches().search(query, buildAbsoluteTimeRange(from, to), limit)
        ));
    }

    @GET @Path("/universal/absolute/terms") @Timed
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
            @ApiParam(title = "to", description = "Timerange end. See search method description for date format", required = true) @QueryParam("to") String to) {
        checkQueryAndField(query, field);

        return json(buildTermsResult(
                core.getIndexer().searches().terms(field, size, query, buildAbsoluteTimeRange(from, to))
        ));
    }

    @GET @Path("/universal/absolute/stats") @Timed
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
            @ApiParam(title = "to", description = "Timerange end. See search method description for date format", required = true) @QueryParam("to") String to) {
        checkQueryAndField(query, field);

        return json(buildFieldStatsResult(
                fieldStats(field, query, buildAbsoluteTimeRange(from, to))
        ));
    }

    @GET @Path("/universal/absolute/histogram") @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String histogramAbsolute(@QueryParam("query") String query, @QueryParam("interval") String interval, @QueryParam("from") String from, @QueryParam("to") String to) {
        interval = interval.toUpperCase();
        checkQueryAndInterval(query, interval);
        validateInterval(interval);

        return json(buildHistogramResult(
                core.getIndexer().searches().histogram(
                        query,
                        Indexer.DateHistogramInterval.valueOf(interval),
                        buildAbsoluteTimeRange(from, to)
                )
        ));
    }





    private void validateInterval(String interval) {
        try {
            Indexer.DateHistogramInterval.valueOf(interval);
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid interval type. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
    }

    private void checkQuery(String query) {
        if (query == null || query.isEmpty()) {
            LOG.error("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
    }

    private void checkQueryAndField(String query, String field) {
        if (field == null || field.isEmpty() || query == null || query.isEmpty()) {
            LOG.warn("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
    }

    private void checkQueryAndInterval(String query, String interval) {
        if (query == null || query.isEmpty() || interval == null || interval.isEmpty()) {
            LOG.warn("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
    }

    private FieldStatsResult fieldStats(String field, String query, TimeRange timeRange) {
        try {
            return core.getIndexer().searches().fieldStats(field, query, timeRange);
        } catch(Searches.FieldTypeException e) {
            LOG.error("Stats query failed. Make sure that field [{}] is a numeric type.", field);
            throw new WebApplicationException(400);
        }
    }

    private TimeRange buildRelativeTimeRange(int range) {
        try {
            return new RelativeRange(range);
        } catch (InvalidRangeParametersException e) {
            LOG.warn("Invalid timerange parameters provided. Returning HTTP 400.");
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

    private Map<String, Object> buildTermsResult(TermsResult tr) {
        Map<String, Object> result = Maps.newHashMap();
        result.put("time", tr.took().millis());
        result.put("terms", tr.getTerms());
        result.put("missing", tr.getMissing()); // The number of docs missing a value.
        result.put("other", tr.getOther()); // The count of terms other than the one provided by the entries.
        result.put("total", tr.getTotal()); // The total count of terms.

        return result;
    }

    private Map<String, Object> buildSearchResult(SearchResult sr) {
        Map<String, Object> result = Maps.newHashMap();
        result.put("query", sr.getOriginalQuery());
        result.put("messages", sr.getResults());
        result.put("fields", sr.getFields());
        result.put("time", sr.took().millis());
        result.put("total_results", sr.getTotalResults());

        return result;
    }

    private Map<String, Object> buildFieldStatsResult(FieldStatsResult sr) {
        Map<String, Object> result = Maps.newHashMap();
        result.put("time", sr.took().millis());
        result.put("count", sr.getCount());
        result.put("sum", sr.getSum());
        result.put("sum_of_squares", sr.getSumOfSquares());
        result.put("mean", sr.getMean());
        result.put("min", sr.getMin());
        result.put("max", sr.getMax());
        result.put("variance", sr.getVariance());
        result.put("std_deviation", sr.getStdDeviation());

        return result;
    }

    private Map<String, Object> buildHistogramResult(DateHistogramResult histogram) {
        Map<String, Object> result = Maps.newHashMap();
        result.put("interval", histogram.getInterval().toString().toLowerCase());
        result.put("results", histogram.getResults());
        result.put("time", histogram.took().millis());

        return result;
    }


}