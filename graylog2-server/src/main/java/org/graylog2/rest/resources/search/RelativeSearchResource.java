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
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.documentation.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Api(value = "Search/Relative", description = "Message search")
@Path("/search/universal/relative")
public class RelativeSearchResource extends SearchResource {

    private static final Logger LOG = LoggerFactory.getLogger(RelativeSearchResource.class);

    @GET @Timed
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

        try {
            return json(buildSearchResult(
                    core.getIndexer().searches().search(query, buildRelativeTimeRange(range), limit)
            ));
        } catch (IndexHelper.InvalidRangeFormatException e) {
            LOG.warn("Invalid timerange parameters provided. Returning HTTP 400.", e);
            throw new WebApplicationException(400);
        }
    }

    @GET @Path("/terms") @Timed
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

        try {
            return json(buildTermsResult(
                    core.getIndexer().searches().terms(field, size, query, buildRelativeTimeRange(range))
            ));
        } catch (IndexHelper.InvalidRangeFormatException e) {
            LOG.warn("Invalid timerange parameters provided. Returning HTTP 400.", e);
            throw new WebApplicationException(400);
        }
    }

    @GET @Path("/stats") @Timed
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

        try {
            return json(buildFieldStatsResult(
                    fieldStats(field, query, buildRelativeTimeRange(range))
            ));
        } catch (IndexHelper.InvalidRangeFormatException e) {
            LOG.warn("Invalid timerange parameters provided. Returning HTTP 400.", e);
            throw new WebApplicationException(400);
        }
    }

    @GET @Path("/histogram") @Timed
    @ApiOperation(value = "Datetime histogram of a query using a relative timerange.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid interval provided."),
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public String histogramRelative(
            @ApiParam(title = "query", description = "Query (Lucene syntax)", required = true) @QueryParam("query") String query,
            @ApiParam(title = "interval", description = "Histogram interval / bucket size. (year, quarter, month, week, day, hour or minute)", required = true) @QueryParam("interval") String interval,
            @ApiParam(title = "range", description = "Relative timeframe to search in. See search method description.", required = true) @QueryParam("range") int range) {
        interval = interval.toUpperCase();
        checkQueryAndInterval(query, interval);
        validateInterval(interval);

        try {
            return json(buildHistogramResult(
                    core.getIndexer().searches().histogram(
                            query,
                            Indexer.DateHistogramInterval.valueOf(interval),
                            buildRelativeTimeRange(range)
                    )
            ));
        } catch (IndexHelper.InvalidRangeFormatException e) {
            LOG.warn("Invalid timerange parameters provided. Returning HTTP 400.", e);
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

}
