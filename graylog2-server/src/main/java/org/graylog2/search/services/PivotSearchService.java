package org.graylog2.search.services;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import org.graylog.events.processor.aggregation.AggregationResult;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog2.inputs.Input;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.graylog.plugins.views.search.rest.scriptingapi.mapping.SearchRequestSpecToSearchMapper.QUERY_ID;


@Singleton
public class PivotSearchService {

    private static final Logger LOG = LoggerFactory.getLogger(PivotSearchService.class);

    private final SearchExecutor searchExecutor;

    @Inject
    public PivotSearchService(SearchExecutor searchExecutor) {
        this.searchExecutor = searchExecutor;
    }
    /**
     * Executes a pivot search based on a query and a pivot field name.
     *
     * @param query          The search query string (e.g., "status:500").
     * @param pivotFieldName The name of the field to pivot on (e.g., "host").
     * @param searchUser     The user performing the search (required for permissions).
     * @return A map where keys are pivot values and values are their counts.
     */
    public Map<String, Long> findPivotValues(String query, String pivotFieldName, SearchUser searchUser) {
        final String queryId = "pivot-query-" + pivotFieldName;
        final String searchTypeId = "pivot-" + pivotFieldName;

        // 1. Building the Search object
        final SearchType pivotSearchType = Pivot.builder()
                .id(searchTypeId)
                .rollup(true)
                .rowGroups(Values.builder().fields(List.of(pivotFieldName)).build())
                .series(Count.builder().build())
                .build();

        final Search search = Search.builder()
                .queries(ImmutableSet.of(
                        Query.builder()
                                .id(queryId)
                                .query(ElasticsearchQueryString.of(query))
                                .searchTypes(Collections.singleton(pivotSearchType))
                                .timerange(RelativeRange.create(900))
                                .build()
                ))
                .build();

        // 2. Executing the search
        final SearchJob searchJob = this.searchExecutor.executeSync(search, searchUser, ExecutionState.empty());

        // 3. Processing the results
        final SearchJobDTO searchJobDTO = SearchJobDTO.fromSearchJob(searchJob);
        final QueryResult queryResult = searchJobDTO.results().get(queryId);

        // Handle search errors
        final Set<SearchError> errors = queryResult.errors();
        if (errors != null && !errors.isEmpty()) {
            String errorMsg = String.format(
                    "An error occurred while executing pivot search for field '%s': %s",
                    pivotFieldName,
                    errors.stream().map(SearchError::description).collect(Collectors.joining(", "))
            );
            LOG.error(errorMsg);
            throw new InternalServerErrorException(errorMsg);
        }

        final AggregationResult aggregationResult = queryResult.searchTypes().get(searchTypeId);

        if (aggregationResult instanceof PivotResult pivotResult) {
            return pivotResult.rows().stream()
                    .filter(row -> row.source().equals("leaf"))
                    .collect(Collectors.toMap(
                            row -> (String) row.key().get("key"),
                            PivotResult.Row::count
                    ));
        }

        LOG.warn("Pivot search for field '{}' returned no results or a non-pivot result type.", pivotFieldName);
        return Collections.emptyMap();
    }
}
