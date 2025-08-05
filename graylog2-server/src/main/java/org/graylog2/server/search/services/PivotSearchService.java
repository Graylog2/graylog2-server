package org.graylog2.server.search.services;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.ws.rs.InternalServerErrorException;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A utility service to simplify execution of basic pivot searches in the backend.
 * Accepts the variable parts (query, pivot field) and handles search building, execution, result parsing, and error handling.
 */
@Singleton
public class PivotSearchService {

    private static final Logger LOG = LoggerFactory.getLogger(PivotSearchService.class);

    private final SearchExecutor searchExecutor;

    @Inject
    public PivotSearchService(SearchExecutor searchExecutor) {
        this.searchExecutor = searchExecutor;
    }

    /**
     * Executes a pivot search for the given query and field.
     *
     * @param query          Search query string (e.g., "status:500").
     * @param pivotFieldName Field to pivot on (e.g., "host").
     * @param searchUser     Search user (permissions).
     * @return Map of pivot value -> count
     */
    public Map<String, Long> findPivotValues(String query, String pivotFieldName, SearchUser searchUser) {
        final String searchTypeId = "pivot-" + pivotFieldName + "-" + UUID.randomUUID();

        // 1. Build Pivot search type
        final SearchType pivotSearchType = Pivot.builder()
                .id(searchTypeId)
                .rollup(true)
                .rowGroups(Values.builder().fields(List.of(pivotFieldName)).build())
                .series(Count.builder().build())
                .build();

        // 2. Build Search object (using ElasticsearchQueryString instead of QueryString)
        final Search search = Search.builder()
                .queries(ImmutableSet.of(
                        Query.builder()
                                .id("pivot-query")
                                .query(ElasticsearchQueryString.of(query))
                                .timerange(RelativeRange.create(900)) // 15 minutes
                                .searchTypes(Collections.singleton(pivotSearchType))
                                .build()
                ))
                .build();

        // 3. Execute search
        final SearchJob searchJob = this.searchExecutor.executeSync(search, searchUser);

        // 4. Check for errors
        final Set<String> errors = searchJob.errors().get("pivot-query");
        if (errors != null && !errors.isEmpty()) {
            String errorMsg = String.format(
                    "Error executing pivot search for field '%s': %s",
                    pivotFieldName,
                    String.join(", ", errors)
            );
            LOG.error(errorMsg);
            throw new InternalServerErrorException(errorMsg);
        }

        // 5. Extract Pivot results
        final var aggregationResult = searchJob.results().get(searchTypeId);
        if (aggregationResult instanceof PivotResult pivotResult) {
            return pivotResult.rows().stream()
                    .filter(row -> row.source().equals("leaf"))
                    .collect(Collectors.toMap(
                            row -> (String) row.key().get("key"),
                            PivotResult.Row::count,
                            Long::sum
                    ));
        }

        LOG.warn("Pivot search for field '{}' returned no results or a non-pivot result type.", pivotFieldName);
        return Collections.emptyMap();
    }
}
