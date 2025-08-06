package org.graylog2.server.search.services;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.ws.rs.InternalServerErrorException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PivotSearchServiceTest {

    @Mock
    private SearchExecutor searchExecutor;

    @Mock
    private SearchUser searchUser;

    private PivotSearchService pivotSearchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pivotSearchService = new PivotSearchService(searchExecutor);
    }

    @Test
    void testFindPivotValues() {
        final SearchJob searchJob = new SearchJob("job-id", null, "user", "node");
        final QueryResult queryResult = QueryResult.builder()
            .query(Query.builder().id("pivot-query").query(ElasticsearchQueryString.of("query")).timerange(RelativeRange.create(900)).build())
            .searchTypes(ImmutableMap.of(
                "pivot-field-name-uuid",
                PivotResult.builder()
                    .id("pivot-field-name-uuid")
                    .rows(ImmutableList.of(
                        PivotResult.Row.builder()
                            .key(ImmutableList.of("value1"))
                            .values(ImmutableList.of(PivotResult.Value.create(List.of(), 10L, false, "leaf")))
                            .source("leaf")
                            .build(),
                        PivotResult.Row.builder()
                            .key(ImmutableList.of("value2"))
                            .values(ImmutableList.of(PivotResult.Value.create(List.of(), 20L, false, "leaf")))
                            .source("leaf")
                            .build()
                    ))
                    .total(2)
                    .build()
            ))
            .build();
        searchJob.addQueryResultFuture("pivot-query", CompletableFuture.completedFuture(queryResult));

        when(searchExecutor.executeSync(any(Search.class), eq(searchUser), any()))
            .thenReturn(searchJob);

        final Map<String, Long> result = pivotSearchService.findPivotValues("query", "field-name", searchUser);

        assertEquals(2, result.size());
        assertEquals(10L, result.get("value1"));
        assertEquals(20L, result.get("value2"));
    }

    @Test
    void testFindPivotValuesWithError() {
        final SearchJob searchJob = new SearchJob("job-id", null, "user", "node");
        searchJob.addError(new SearchError(null, "error description"));

        when(searchExecutor.executeSync(any(Search.class), eq(searchUser), any()))
            .thenReturn(searchJob);

        assertThrows(InternalServerErrorException.class, () -> {
            pivotSearchService.findPivotValues("query", "field-name", searchUser);
        });
    }
}
