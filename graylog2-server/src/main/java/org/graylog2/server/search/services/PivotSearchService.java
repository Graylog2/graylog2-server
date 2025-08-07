package org.graylog2.server.search.services;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.InternalServerErrorException;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Utility service for executing basic pivot search queries.
 * Accepts a query string and pivot field name, builds and executes the search,
 * handles errors, and extracts pivot values.
 */
@Singleton
public class PivotSearchService {
    private static final Logger LOG = LoggerFactory.getLogger(PivotSearchService.class);
    private final SearchExecutor searchExecutor;
    private final Supplier<UUID> uuidSupplier;

    @Inject
    public PivotSearchService(SearchExecutor searchExecutor) {
        this(searchExecutor, UUID::randomUUID);
    }

    @VisibleForTesting
    PivotSearchService(SearchExecutor searchExecutor, Supplier<UUID> uuidSupplier) {
        this.searchExecutor = searchExecutor;
        this.uuidSupplier = uuidSupplier;
    }

    public Map<String, Long> findPivotValues(String query, String pivotFieldName, SearchUser searchUser) {
        final String searchTypeId = "pivot-" + pivotFieldName + "-" + uuidSupplier.get();
        final String queryId = "pivot-query";

        final Pivot pivotSearchType = Pivot.builder()
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
                    .timerange(RelativeRange.create(900))
                    .searchTypes(Collections.singleton(pivotSearchType))
                    .build()))
            .build();

        final SearchJob job = this.searchExecutor.executeSync(search, searchUser, null);

        // 4. Check for errors
        final Set<SearchError> errors = job.getErrors();
        if (errors != null && !errors.isEmpty()) {
            final String err = "Error executing pivot search for field '" + pivotFieldName + "': " + errors.stream().map(SearchError::description).collect(Collectors.joining(", "));
            LOG.error(err);
            throw new InternalServerErrorException(err);
        }

        // 5. Extract Pivot results
        final Optional<QueryResult> queryResult = Optional.ofNullable(job.results().get(queryId));
        if (queryResult.isPresent()) {
            final SearchType.Result result = queryResult.get().searchTypes().get(searchTypeId);
            if (result instanceof PivotResult) {
                final PivotResult pivotResult = (PivotResult) result;
                return pivotResult.rows().stream()
                    .filter(r -> "leaf".equals(r.source()))
                    .collect(Collectors.toMap(
                        r -> r.key().get(0),
                        (row) -> (Long) row.values().get(0).value(),
                        Long::sum));
            }
        }

        LOG.warn("Pivot result is not of expected type for field '{}'", pivotFieldName);
        return Collections.emptyMap();
    }
}