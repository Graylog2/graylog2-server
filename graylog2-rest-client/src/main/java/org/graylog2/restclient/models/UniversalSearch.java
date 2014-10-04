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
package org.graylog2.restclient.models;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.ApiRequestBuilder;
import org.graylog2.restclient.lib.timeranges.TimeRange;
import org.graylog2.restclient.models.api.responses.*;
import org.graylog2.restclient.models.api.responses.system.indices.IndexSummaryResponse;
import org.graylog2.restclient.models.api.results.DateHistogramResult;
import org.graylog2.restclient.models.api.results.SearchResult;
import org.graylog2.restroutes.PathMethod;
import org.graylog2.restroutes.generated.routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.graylog2.restclient.lib.Configuration.apiTimeout;

public class UniversalSearch {
    private static final Logger log = LoggerFactory.getLogger(UniversalSearch.class);

    public static final int PER_PAGE = 100;
    public static final int KEITH = 61;  // http://en.wikipedia.org/wiki/61_(number) -> Keith

    public static final SearchSort DEFAULT_SORT = new SearchSort("timestamp", SearchSort.Direction.DESC);

    private final ApiClient api;
    private final String query;
    private final FieldMapper fieldMapper;
    private final String filter;
    private final TimeRange timeRange;
    private final Integer page;
    private final SearchSort order;

    @AssistedInject
    private UniversalSearch(ApiClient api, FieldMapper fieldMapper, @Assisted TimeRange timeRange, @Assisted String query) {
        this(api, fieldMapper, timeRange, query, 0);
    }

    @AssistedInject
    private UniversalSearch(ApiClient api, FieldMapper fieldMapper, @Assisted TimeRange timeRange, @Assisted String query, @Assisted Integer page) {
        this(api, fieldMapper, timeRange, query, page, null, null);
    }

    @AssistedInject
    private UniversalSearch(ApiClient api, FieldMapper fieldMapper, @Assisted String query, @Assisted TimeRange timeRange, @Nullable @Assisted("filter") String filter) {
        this(api, fieldMapper, timeRange, query, 0, filter, null);
    }

    @AssistedInject
    public UniversalSearch(ApiClient api, FieldMapper fieldMapper, @Assisted TimeRange timeRange, @Assisted("query") String query, @Assisted Integer page, @Assisted("filter") String filter) {
        this(api, fieldMapper,timeRange, query, page, filter, null);
    }

    @AssistedInject
    public UniversalSearch(ApiClient api, FieldMapper fieldMapper, @Assisted TimeRange timeRange, @Assisted String query, @Assisted Integer page, @Assisted SearchSort order) {
        this(api, fieldMapper, timeRange, query, page, null, order);
    }

    @AssistedInject
    private UniversalSearch(ApiClient api, FieldMapper fieldMapper, @Assisted TimeRange timeRange, @Assisted("query") String query, @Assisted Integer page, @Assisted("filter") String filter, @Assisted SearchSort order) {
        this.fieldMapper = fieldMapper;
        this.filter = filter;
        this.api = api;
        this.query = query;
        this.timeRange = timeRange;

        if (page == 0) {
            this.page = 0;
        } else {
            this.page = page - 1;
        }

        if (order == null) {
            this.order = DEFAULT_SORT;
        } else {
            this.order = order;
        }
    }

    private <T> T doSearch(Class<T> clazz, MediaType mediaType, int pageSize, Set<String> selectedFields) throws APIException, IOException {
        PathMethod pathMethod;
        switch (timeRange.getType()) {
            case ABSOLUTE:
                pathMethod = routes.AbsoluteSearchResource().searchAbsolute();
                break;
            case KEYWORD:
                pathMethod = routes.KeywordSearchResource().searchKeyword();
                break;
            case RELATIVE:
                pathMethod = routes.RelativeSearchResource().searchRelative();
                break;
            default:
                throw new RuntimeException("Invalid time range type!");
        }
        final ApiRequestBuilder<T> builder =  api.path(pathMethod, clazz)
                .queryParams(timeRange.getQueryParams())
                .queryParam("query", query)
                .queryParam("limit", pageSize)
                .queryParam("offset", page * pageSize)
                .queryParam("filter", (filter == null ? "*" : filter))
                .queryParam("sort", order.toApiParam());
        if (selectedFields != null && !selectedFields.isEmpty()) {
            builder.queryParam("fields", Joiner.on(',').skipNulls().join(selectedFields));
        }
        final T result = builder
                .accept(mediaType)
                .timeout(KEITH, TimeUnit.SECONDS)
                .expect(200, 400)
                .execute();
        return result;
    }

    public SearchResult search() throws IOException, APIException {
        SearchResultResponse response = doSearch(SearchResultResponse.class, MediaType.JSON_UTF_8, PER_PAGE, null);
        if (response == null) {
            log.error("We should never get an empty result without throwing an IOException.");
            throw new APIException(null, null, new RuntimeException("Empty search response, this is likely a bug in exception handling."));
        }

        SearchResult result = new SearchResult(
                query,
                response.builtQuery,
                timeRange,
                response.total_results,
                response.time,
                response.messages,
                response.fields,
                response.usedIndices,
                response.error != null ? response.error : response.genericError,
                response.getFromDataTime(),
                response.getToDataTime(),
                fieldMapper
        );

        return result;
    }

    public String searchAsCsv(Set<String> selectedFields) throws IOException, APIException {
        return doSearch(String.class, MediaType.CSV_UTF_8, 10_000, selectedFields);  // TODO make use of streaming support in the server.
    }

    public DateHistogramResult dateHistogram(String interval) throws IOException, APIException {
        PathMethod routePath;
        switch (timeRange.getType()) {
            case ABSOLUTE:
                routePath = routes.AbsoluteSearchResource().histogramAbsolute();
                break;
            case KEYWORD:
                routePath = routes.KeywordSearchResource().histogramKeyword();
                break;
            case RELATIVE:
                routePath = routes.RelativeSearchResource().histogramRelative();
                break;
            default:
                throw new RuntimeException("Invalid time range type!");
        }
        DateHistogramResponse response = api.path(routePath, DateHistogramResponse.class)
                .queryParam("interval", interval)
                .queryParam("query", query)
                .queryParams(timeRange.getQueryParams())
                .queryParam("filter", (filter == null ? "*" : filter))
                .timeout(apiTimeout("search_universal_histogram", KEITH, TimeUnit.SECONDS))
                .execute();
        return new DateHistogramResult(
                response.query,
                response.time,
                response.interval,
                response.results,
                response.getHistogramBoundaries(),
                timeRange
        );
    }

    public FieldStatsResponse fieldStats(String field) throws IOException, APIException {
        PathMethod routePath;
        switch (timeRange.getType()) {
            case ABSOLUTE:
                routePath = routes.AbsoluteSearchResource().statsAbsolute();
                break;
            case KEYWORD:
                routePath = routes.KeywordSearchResource().statsKeyword();
                break;
            case RELATIVE:
                routePath = routes.RelativeSearchResource().statsRelative();
                break;
            default:
                throw new RuntimeException("Invalid time range type!");
        }
        return api.path(routePath, FieldStatsResponse.class)
                .queryParam("field", field)
                .queryParam("query", query)
                .queryParams(timeRange.getQueryParams())
                .queryParam("filter", (filter == null ? "*" : filter))
                .timeout(apiTimeout("search_universal_stats", KEITH, TimeUnit.SECONDS))
                .execute();
    }

    public FieldTermsResponse fieldTerms(String field) throws IOException, APIException {
        PathMethod routePath;
        switch (timeRange.getType()) {
            case ABSOLUTE:
                routePath = routes.AbsoluteSearchResource().termsAbsolute();
                break;
            case KEYWORD:
                routePath = routes.KeywordSearchResource().termsKeyword();
                break;
            case RELATIVE:
                routePath = routes.RelativeSearchResource().termsRelative();
                break;
            default:
                throw new RuntimeException("Invalid time range type!");
        }
        return api.path(routePath, FieldTermsResponse.class)
                .queryParam("field", field)
                .queryParam("query", query)
                .queryParams(timeRange.getQueryParams())
                .queryParam("filter", (filter == null ? "*" : filter))
                .timeout(apiTimeout("search_universal_terms", KEITH, TimeUnit.SECONDS))
                .execute();
    }

    public FieldHistogramResponse fieldHistogram(String field, String interval) throws IOException, APIException {
        PathMethod routePath;
        switch (timeRange.getType()) {
            case ABSOLUTE:
                routePath = routes.AbsoluteSearchResource().fieldHistogramAbsolute();
                break;
            case KEYWORD:
                routePath = routes.KeywordSearchResource().fieldHistogramKeyword();
                break;
            case RELATIVE:
                routePath = routes.RelativeSearchResource().fieldHistogramRelative();
                break;
            default:
                throw new RuntimeException("Invalid time range type!");
        }
        return api.path(routePath, FieldHistogramResponse.class)
                .queryParam("field", field)
                .queryParam("interval", interval)
                .queryParam("query", query)
                .queryParams(timeRange.getQueryParams())
                .queryParam("filter", (filter == null ? "*" : filter))
                .timeout(apiTimeout("search_universal_fieldhistogram", KEITH, TimeUnit.SECONDS))
                .execute();
    }

    public SearchSort getOrder() {
        return order;
    }

    public String getQuery() {
        return query;
    }

    public String getFilter() {
        return filter;
    }

    public TimeRange getTimeRange() {
        return timeRange;
    }

    public interface Factory {
        UniversalSearch queryWithRange(String query, TimeRange timeRange);

        UniversalSearch queryWithRangeAndFilter(String query, TimeRange timeRange, @Assisted("filter") String filter);

        UniversalSearch queryWithRangePageAndOrder(String query, TimeRange timeRange, Integer page, SearchSort order);

        UniversalSearch queryWithFilterRangeAndPage(@Assisted("query") String query, @Assisted("filter") String filter, TimeRange timeRange, Integer page);

        UniversalSearch queryWithFilterRangePageAndOrder(@Assisted("query") String query, @Assisted("filter") String filter, TimeRange timeRange, Integer page, SearchSort order);
    }

}
