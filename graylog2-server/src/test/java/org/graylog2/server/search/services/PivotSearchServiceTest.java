package org.graylog2.server.search.services;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.ws.rs.InternalServerErrorException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PivotSearchServiceTest {

    @Mock
    private SearchExecutor searchExecutor;

    @Mock
    private SearchUser searchUser;

    private PivotSearchService pivotSearchService;
    private final UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pivotSearchService = new PivotSearchService(searchExecutor, () -> uuid);
    }

    @Test
    void testFindPivotValues() {
        Search mockSearch = mock(Search.class);
        when(mockSearch.id()).thenReturn("search-id");

        final SearchJob searchJob = new SearchJob("job-id", mockSearch, "user", "node");

        Query mockQuery = mock(Query.class);
        when(mockQuery.id()).thenReturn("pivot-query");

        final QueryResult queryResult = QueryResult.emptyResult().toBuilder()
                .query(mockQuery)
                .searchTypes(ImmutableMap.of(
                        "pivot-field-name-" + uuid,
                        PivotResult.builder()
                                .id("pivot-field-name-" + uuid)
                                .rows(ImmutableList.of(
                                        PivotResult.Row.builder()
                                                .key(ImmutableList.of("value1"))
                                                .values(ImmutableList.of(PivotResult.Value.create(List.of(), 10L, false, "count")))
                                                .source("leaf")
                                                .build(),
                                        PivotResult.Row.builder()
                                                .key(ImmutableList.of("value2"))
                                                .values(ImmutableList.of(PivotResult.Value.create(List.of(), 20L, false, "count")))
                                                .source("leaf")
                                                .build()
                                ))
                                .total(2)
                                .effectiveTimerange(AbsoluteRange.create("2025-08-07 10:00:00", "2025-08-07 11:00:00"))
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
        Search mockSearch = mock(Search.class);
        when(mockSearch.id()).thenReturn("search-id");

        final SearchJob searchJob = new SearchJob("job-id", mockSearch, "test-user", "test-node");
        searchJob.addError(() -> "error description");

        when(searchExecutor.executeSync(any(Search.class), eq(searchUser), any()))
                .thenReturn(searchJob);

        assertThrows(InternalServerErrorException.class, () -> {
            pivotSearchService.findPivotValues("query", "field-name", searchUser);
        });
    }
}