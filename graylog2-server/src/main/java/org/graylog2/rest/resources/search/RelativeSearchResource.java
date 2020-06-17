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
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.rest.models.search.responses.FieldStatsResult;
import org.graylog2.rest.models.search.responses.HistogramResult;
import org.graylog2.rest.models.search.responses.TermsHistogramResult;
import org.graylog2.rest.models.search.responses.TermsResult;
import org.graylog2.rest.models.search.responses.TermsStatsResult;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.utilities.SearchUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.PositiveOrZero;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.graylog2.utilities.SearchUtils.buildTermsHistogramResult;

@RequiresAuthentication
@Api(value = "Legacy/Search/Relative", description = "Message search")
@Path("/search/universal/relative")
public class RelativeSearchResource extends SearchResource {

    private static final Logger LOG = LoggerFactory.getLogger(RelativeSearchResource.class);

    @Inject
    public RelativeSearchResource(Searches searches,
                                  ClusterConfigService clusterConfigService,
                                  DecoratorProcessor decoratorProcessor) {
        super(searches, clusterConfigService, decoratorProcessor);
    }

    @GET
    @Timed
    @ApiOperation(value = "Message search with relative timerange.",
            notes = "Search for messages in a relative timerange, specified as seconds from now. " +
                    "Example: 300 means search from 5 minutes ago to now.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public SearchResponse searchRelative(
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "range", value = "Relative timeframe to search in. See method description.", required = true)
            @QueryParam("range") @PositiveOrZero int range,
            @ApiParam(name = "limit", value = "Maximum number of messages to return.", required = false) @QueryParam("limit") int limit,
            @ApiParam(name = "offset", value = "Offset", required = false) @QueryParam("offset") int offset,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter,
            @ApiParam(name = "fields", value = "Comma separated list of fields to return", required = false) @QueryParam("fields") String fields,
            @ApiParam(name = "sort", value = "Sorting (field:asc / field:desc)", required = false) @QueryParam("sort") String sort,
            @ApiParam(name = "decorate", value = "Run decorators on search result", required = false) @QueryParam("decorate") @DefaultValue("true") boolean decorate) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_RELATIVE);

        final List<String> fieldList = parseOptionalFields(fields);
        final Sorting sorting = buildSorting(sort);

        final TimeRange timeRange = buildRelativeTimeRange(range);
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
    @ApiOperation(value = "Message search with relative timerange.",
            notes = "Search for messages in a relative timerange, specified as seconds from now. " +
                    "Example: 300 means search from 5 minutes ago to now.")
    @Produces(MoreMediaTypes.TEXT_CSV)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    public ChunkedOutput<ScrollResult.ScrollChunk> searchRelativeChunked(
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "range", value = "Relative timeframe to search in. See method description.", required = true)
            @QueryParam("range") @PositiveOrZero int range,
            @ApiParam(name = "limit", value = "Maximum number of messages to return.", required = false) @QueryParam("limit") int limit,
            @ApiParam(name = "offset", value = "Offset", required = false) @QueryParam("offset") int offset,
            @ApiParam(name = "batch_size", value = "Batch size for the backend storage export request.", required = false) @QueryParam("batch_size") @DefaultValue(DEFAULT_SCROLL_BATCH_SIZE) int batchSize,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter,
            @ApiParam(name = "fields", value = "Comma separated list of fields to return", required = true)
            @QueryParam("fields") @NotEmpty String fields) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_RELATIVE);

        final List<String> fieldList = parseFields(fields);
        final TimeRange timeRange = buildRelativeTimeRange(range);

        final ScrollResult scroll = searches
                .scroll(query, timeRange, batchSize, offset, fieldList, filter);
        return buildChunkedOutput(scroll, limit);
    }

    @GET
    @Path("/export")
    @Timed
    @ApiOperation(value = "Export message search with relative timerange.",
            notes = "Search for messages in a relative timerange, specified as seconds from now. " +
                    "Example: 300 means search from 5 minutes ago to now.")
    @Produces(MoreMediaTypes.TEXT_CSV)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    public Response exportSearchRelativeChunked(
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "range", value = "Relative timeframe to search in. See method description.", required = true)
            @QueryParam("range") @PositiveOrZero int range,
            @ApiParam(name = "limit", value = "Maximum number of messages to return.", required = false) @QueryParam("limit") int limit,
            @ApiParam(name = "offset", value = "Offset", required = false) @QueryParam("offset") int offset,
            @ApiParam(name = "batch_size", value = "Batch size for the backend storage export request.", required = false) @QueryParam("batch_size") @DefaultValue(DEFAULT_SCROLL_BATCH_SIZE) int batchSize,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter,
            @ApiParam(name = "fields", value = "Comma separated list of fields to return", required = true)
            @QueryParam("fields") @NotEmpty String fields) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_RELATIVE);
        final String filename = "graylog-search-result-relative-" + range + ".csv";
        return Response
            .ok(searchRelativeChunked(query, range, limit, offset, batchSize, filter, fields))
            .header("Content-Disposition", "attachment; filename=" + filename)
            .build();
    }

    @Deprecated
    @GET
    @Path("/terms")
    @Timed
    @ApiOperation(value = "Most common field terms of a query using a relative timerange.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public TermsResult termsRelative(
            @ApiParam(name = "field", value = "Message field of to return terms of", required = true)
            @QueryParam("field") @NotEmpty String field,
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "stacked_fields", value = "Fields to stack", required = false) @QueryParam("stacked_fields") String stackedFieldsParam,
            @ApiParam(name = "size", value = "Maximum number of terms to return", required = false) @QueryParam("size") int size,
            @ApiParam(name = "range", value = "Relative timeframe to search in. See search method description.", required = true)
            @QueryParam("range") @PositiveOrZero int range,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter,
            @ApiParam(name = "order", value = "Sorting (field:asc / field:desc)", required = false) @QueryParam("order") String order) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_RELATIVE);

        final List<String> stackedFields = splitStackedFields(stackedFieldsParam);
        final Sorting sortOrder = buildSorting(order);
        return buildTermsResult(searches.terms(field, stackedFields, size, query, filter, buildRelativeTimeRange(range), sortOrder.getDirection()));
    }

    @Deprecated
    @GET
    @Path("/terms-histogram")
    @Timed
    @ApiOperation(value = "Most common field terms of a query over time using a relative timerange.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public TermsHistogramResult termsHistogramRelative(
            @ApiParam(name = "field", value = "Message field of to return terms of", required = true)
            @QueryParam("field") @NotEmpty String field,
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "stacked_fields", value = "Fields to stack", required = false) @QueryParam("stacked_fields") String stackedFieldsParam,
            @ApiParam(name = "size", value = "Maximum number of terms to return", required = true)
            @QueryParam("size") @Min(1) int size,
            @ApiParam(name = "range", value = "Relative timeframe to search in. See search method description.", required = true)
            @QueryParam("range") @PositiveOrZero int range,
            @ApiParam(name = "interval", value = "Histogram interval / bucket size. (year, quarter, month, week, day, hour or minute)", required = false)
            @QueryParam("interval") String interval,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter,
            @ApiParam(name = "order", value = "Sorting (field:asc / field:desc)", required = false) @QueryParam("order") String order) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_RELATIVE);

        final List<String> stackedFields = splitStackedFields(stackedFieldsParam);
        final Sorting sortOrder = buildSorting(order);
        final TimeRange timeRange = buildRelativeTimeRange(range);

        return buildTermsHistogramResult(searches.termsHistogram(field, stackedFields, size, query, filter, timeRange, SearchUtils.buildInterval(interval, timeRange), sortOrder.getDirection()));
    }

    @Deprecated
    @GET
    @Path("/termsstats")
    @Timed
    @ApiOperation(value = "Ordered field terms of a query computed on another field using a relative timerange.")
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
            @ApiParam(name = "range", value = "Relative timeframe to search in. See search method description.", required = true)
            @QueryParam("range") @PositiveOrZero int range,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_RELATIVE);

        return buildTermsStatsResult(
                searches.termsStats(keyField, valueField, Searches.TermsStatsOrder.valueOf(order.toUpperCase(Locale.ENGLISH)), size, query, filter, buildRelativeTimeRange(range))
        );
    }

    @Deprecated
    @GET
    @Path("/stats")
    @Timed
    @ApiOperation(value = "Field statistics for a query using a relative timerange.",
            notes = "Returns statistics like min/max or standard deviation of numeric fields " +
                    "over the whole query result set.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided."),
            @ApiResponse(code = 400, message = "Field is not of numeric type.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public FieldStatsResult statsRelative(
            @ApiParam(name = "field", value = "Message field of numeric type to return statistics for", required = true)
            @QueryParam("field") @NotEmpty String field,
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "range", value = "Relative timeframe to search in. See search method description.", required = true)
            @QueryParam("range") @PositiveOrZero int range,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_RELATIVE);

        return buildFieldStatsResult(fieldStats(field, query, filter, buildRelativeTimeRange(range)));
    }

    @Deprecated
    @GET
    @Path("/histogram")
    @Timed
    @ApiOperation(value = "Datetime histogram of a query using a relative timerange.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid interval provided."),
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public HistogramResult histogramRelative(
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "interval", value = "Histogram interval / bucket size. (year, quarter, month, week, day, hour or minute)", required = true)
            @QueryParam("interval") @NotEmpty String interval,
            @ApiParam(name = "range", value = "Relative timeframe to search in. See search method description.", required = true)
            @QueryParam("range") @PositiveOrZero int range,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_RELATIVE);

        interval = interval.toUpperCase(Locale.ENGLISH);
        validateInterval(interval);

        return buildHistogramResult(
                searches.histogram(
                        query,
                        Searches.DateHistogramInterval.valueOf(interval),
                        filter,
                        buildRelativeTimeRange(range)
                )
        );
    }

    @Deprecated
    @GET
    @Path("/fieldhistogram")
    @Timed
    @ApiOperation(value = "Field value histogram of a query using a relative timerange.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid interval provided."),
            @ApiResponse(code = 400, message = "Invalid timerange parameters provided."),
            @ApiResponse(code = 400, message = "Field is not of numeric type.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public HistogramResult fieldHistogramRelative(
            @ApiParam(name = "query", value = "Query (Lucene syntax)", required = true)
            @QueryParam("query") @NotEmpty String query,
            @ApiParam(name = "field", value = "Field of whose values to get the histogram of", required = true)
            @QueryParam("field") @NotEmpty String field,
            @ApiParam(name = "interval", value = "Histogram interval / bucket size. (year, quarter, month, week, day, hour or minute)", required = true)
            @QueryParam("interval") @NotEmpty String interval,
            @ApiParam(name = "range", value = "Relative timeframe to search in. See search method description.", required = true)
            @QueryParam("range") @PositiveOrZero int range,
            @ApiParam(name = "filter", value = "Filter", required = false) @QueryParam("filter") String filter,
            @ApiParam(name = "cardinality", value = "Calculate the cardinality of the field as well", required = false) @QueryParam("cardinality") boolean includeCardinality
    ) {
        checkSearchPermission(filter, RestPermissions.SEARCHES_RELATIVE);

        interval = interval.toUpperCase(Locale.ENGLISH);
        validateInterval(interval);

        return buildHistogramResult(fieldHistogram(field, query, interval, filter, buildRelativeTimeRange(range),
                                                   includeCardinality));
    }

    private TimeRange buildRelativeTimeRange(int range) {
        try {
            return restrictTimeRange(RelativeRange.create(range));
        } catch (InvalidRangeParametersException e) {
            LOG.warn("Invalid timerange parameters provided. Returning HTTP 400.");
            throw new BadRequestException(e);
        }
    }
}
