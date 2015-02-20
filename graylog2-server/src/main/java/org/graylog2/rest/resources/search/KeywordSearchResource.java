/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.search;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.glassfish.jersey.server.ChunkedOutput;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.SearchesConfig;
import org.graylog2.indexer.searches.SearchesConfigBuilder;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.KeywordRange;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.resources.search.responses.FieldStatsResult;
import org.graylog2.rest.resources.search.responses.HistogramResult;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.graylog2.rest.resources.search.responses.TermsResult;
import org.graylog2.rest.resources.search.responses.TermsStatsResult;
import org.graylog2.shared.rest.AdditionalMediaType;
import org.graylog2.shared.security.RestPermissions;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@RequiresAuthentication
@Api(value = "Search/Keyword", description = "Message search")
@Path("/search/universal/keyword")
public class KeywordSearchResource extends SearchResource {

    private static final Logger LOG = LoggerFactory.getLogger(KeywordSearchResource.class);

    @Inject
    public KeywordSearchResource(Searches searches) {
        super(searches);
    }

    @GET
    @Timed
    @ApiOperation(value = "Message search with keyword as timerange.",
            notes = "Search for messages in a timerange defined by a keyword like \"yesterday\" or \"2 weeks ago to wednesday\".")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid keyword provided.")
    })
    public SearchResponse searchKeyword(
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "keyword", value = "Range keyword", required = true)
            @QueryParam("keyword") @NotEmpty String keyword,
            @ApiParam(name = "limit", value = "Maximum number of messages to return.", required = false) @QueryParam("limit") int limit,
            @ApiParam(name = "offset", value = "Offset", required = false) @QueryParam("offset") int offset,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter,
            @ApiParam(name = "fields", value = "Comma separated list of fields to return", required = false) @QueryParam("fields") String fields,
            @ApiParam(name = "sort", value = "Sorting (field:asc / field:desc)", required = false) @QueryParam("sort") String sort) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_KEYWORD);

        final List<String> fieldList = parseOptionalFields(fields);
        final Sorting sorting = buildSorting(sort);
        final TimeRange timeRange = buildKeywordTimeRange(keyword);
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
        } catch (SearchPhaseExecutionException e) {
            throw createRequestExceptionForParseFailure(query, e);
        }
    }

    @GET
    @Timed
    @ApiOperation(value = "Message search with keyword as timerange.",
            notes = "Search for messages in a timerange defined by a keyword like \"yesterday\" or \"2 weeks ago to wednesday\".")
    @Produces(AdditionalMediaType.TEXT_CSV)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid keyword provided.")
    })
    public ChunkedOutput<ScrollResult.ScrollChunk> searchKeywordChunked(
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "keyword", value = "Range keyword", required = true) @QueryParam("keyword") String keyword,
            @ApiParam(name = "limit", value = "Maximum number of messages to return.", required = false) @QueryParam("limit") int limit,
            @ApiParam(name = "offset", value = "Offset", required = false) @QueryParam("offset") int offset,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter,
            @ApiParam(name = "fields", value = "Comma separated list of fields to return", required = true) @QueryParam("fields") String fields) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_KEYWORD);

        final List<String> fieldList = parseFields(fields);
        final TimeRange timeRange = buildKeywordTimeRange(keyword);

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
        }
    }

    @GET
    @Path("/histogram")
    @Timed
    @ApiOperation(value = "Datetime histogram of a query using keyword timerange.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided."),
            @ApiResponse(code = 400, message = "Invalid interval provided.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public HistogramResult histogramKeyword(
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "interval", value = "Histogram interval / bucket size. (year, quarter, month, week, day, hour or minute)", required = true)
            @QueryParam("interval") @NotEmpty String interval,
            @ApiParam(name = "keyword", value = "Range keyword", required = true) @QueryParam("keyword") String keyword,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_KEYWORD);

        interval = interval.toUpperCase();
        validateInterval(interval);

        try {
            return buildHistogramResult(
                    searches.histogram(
                            query,
                            Searches.DateHistogramInterval.valueOf(interval),
                            filter,
                            buildKeywordTimeRange(keyword)
                    )
            );
        } catch (SearchPhaseExecutionException e) {
            throw createRequestExceptionForParseFailure(query, e);
        }
    }

    @GET
    @Path("/terms")
    @Timed
    @ApiOperation(value = "Most common field terms of a query using a keyword timerange.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public TermsResult termsKeyword(
            @ApiParam(name = "field", value = "Message field of to return terms of", required = true)
            @QueryParam("field") @NotEmpty String field,
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "size", value = "Maximum number of terms to return", required = false) @QueryParam("size") int size,
            @ApiParam(name = "keyword", value = "Range keyword", required = true) @QueryParam("keyword") String keyword,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_KEYWORD);

        try {
            return buildTermsResult(searches.terms(field, size, query, filter, buildKeywordTimeRange(keyword)));
        } catch (SearchPhaseExecutionException e) {
            throw createRequestExceptionForParseFailure(query, e);
        }
    }

    @GET
    @Path("/termsstats")
    @Timed
    @ApiOperation(value = "Ordered field terms of a query computed on another field using a keyword timerange.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public TermsStatsResult termsStatsRelative(
            @ApiParam(name = "key_field", value = "Message field of to return terms of", required = true)
            @QueryParam("key_field") @NotEmpty String keyField,
            @ApiParam(name = "value_field", value = "Value field used for computation", required = true)
            @QueryParam("value_field") @NotEmpty String valueField,
            @ApiParam(name = "order", value = "What to order on (Allowed values: TERM, REVERSE_TERM, COUNT, REVERSE_COUNT, TOTAL, REVERSE_TOTAL, MIN, REVERSE_MIN, MAX, REVERSE_MAX, MEAN, REVERSE_MEAN)", required = true)
            @QueryParam("order") @NotEmpty String order,
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "size", value = "Maximum number of terms to return", required = false) @QueryParam("size") int size,
            @ApiParam(name = "keyword", value = "Keyword timeframe", required = true) @QueryParam("keyword") String keyword,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_KEYWORD);

        try {
            return buildTermsStatsResult(
                    searches.termsStats(keyField, valueField, Searches.TermsStatsOrder.valueOf(order.toUpperCase()), size, query, filter, buildKeywordTimeRange(keyword))
            );
        } catch (SearchPhaseExecutionException e) {
            throw createRequestExceptionForParseFailure(query, e);
        }
    }

    @GET
    @Path("/stats")
    @Timed
    @ApiOperation(value = "Field statistics for a query using a keyword timerange.",
            notes = "Returns statistics like min/max or standard deviation of numeric fields " +
                    "over the whole query result set.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided."),
            @ApiResponse(code = 400, message = "Field is not of numeric type.")
    })
    public FieldStatsResult statsKeyword(
            @ApiParam(name = "field", value = "Message field of numeric type to return statistics for", required = true)
            @QueryParam("field") @NotEmpty String field,
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "keyword", value = "Range keyword", required = true) @QueryParam("keyword") String keyword,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_KEYWORD);

        try {
            return buildFieldStatsResult(fieldStats(field, query, filter, buildKeywordTimeRange(keyword)));
        } catch (SearchPhaseExecutionException e) {
            throw createRequestExceptionForParseFailure(query, e);
        }
    }

    @GET
    @Path("/fieldhistogram")
    @Timed
    @ApiOperation(value = "Datetime histogram of a query using keyword timerange.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided."),
            @ApiResponse(code = 400, message = "Invalid interval provided."),
            @ApiResponse(code = 400, message = "Field is not of numeric type.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public HistogramResult fieldHistogramKeyword(
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "field", value = "Field of whose values to get the histogram of", required = true)
            @QueryParam("field") @NotEmpty String field,
            @ApiParam(name = "interval", value = "Histogram interval / bucket size. (year, quarter, month, week, day, hour or minute)", required = true)
            @QueryParam("interval") @NotEmpty String interval,
            @ApiParam(name = "keyword", value = "Range keyword", required = true) @QueryParam("keyword") String keyword,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_KEYWORD);

        interval = interval.toUpperCase();
        validateInterval(interval);

        try {
            return buildHistogramResult(fieldHistogram(field, query, interval, filter, buildKeywordTimeRange(keyword)));
        } catch (SearchPhaseExecutionException e) {
            throw createRequestExceptionForParseFailure(query, e);
        }
    }

    private TimeRange buildKeywordTimeRange(String keyword) {
        try {
            return new KeywordRange(keyword);
        } catch (InvalidRangeParametersException e) {
            LOG.warn("Invalid timerange parameters provided. Returning HTTP 400.");
            throw new BadRequestException("Invalid timerange parameters provided", e);
        }
    }
}