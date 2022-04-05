/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.views.search.db;

import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchSummary;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewResolver;
import org.graylog.plugins.views.search.views.ViewSummaryDTO;
import org.graylog.plugins.views.search.views.ViewSummaryService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchesCleanUpJobTest {
    public static final String IN_USE_SEARCH_ID = "This search is in use";
    public static final String IN_USE_RESOLVER_SEARCH_ID = "in-use-resolver-search-id";
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ViewSummaryService viewService;

    @Mock
    private SearchDbService searchDbService;

    private SearchesCleanUpJob searchesCleanUpJob;

    @Before
    public void setup() {
        this.searchesCleanUpJob = new SearchesCleanUpJob(viewService, searchDbService, Duration.standardDays(4), testViewResolvers());
    }

    @Test
    public void testForAllEmpty() {
        when(viewService.streamAll()).thenReturn(Stream.empty());
        when(searchDbService.streamAll()).thenReturn(Stream.empty());

        this.searchesCleanUpJob.doRun();

        verify(searchDbService, never()).delete(any());
    }

    @Test
    public void testForEmptySearches() {
        final ViewSummaryDTO view = mock(ViewSummaryDTO.class);

        when(viewService.streamAll()).thenReturn(Stream.of(view));
        when(searchDbService.streamAll()).thenReturn(Stream.empty());

        this.searchesCleanUpJob.doRun();

        verify(searchDbService, never()).delete(any());
    }

    @Test
    public void testForNonexpiredSearches() {
        when(viewService.streamAll()).thenReturn(Stream.empty());

        final Search search1 = mock(Search.class);
        when(search1.createdAt()).thenReturn(DateTime.now(DateTimeZone.UTC).minus(Duration.standardDays(1)));
        final Search search2 = mock(Search.class);
        when(search2.createdAt()).thenReturn(DateTime.now(DateTimeZone.UTC).minus(Duration.standardHours(4)));

        when(searchDbService.streamAll()).thenReturn(Stream.of(search1, search2));

        this.searchesCleanUpJob.doRun();

        verify(searchDbService, never()).delete(any());
    }

    @Test
    public void testForReferencedSearches() {
        final ViewSummaryDTO view = mock(ViewSummaryDTO.class);
        when(view.searchId()).thenReturn(IN_USE_SEARCH_ID);
        when(viewService.streamAll()).thenReturn(Stream.of(view));

        final Search search = mock(Search.class);
        when(search.createdAt()).thenReturn(DateTime.now(DateTimeZone.UTC).minus(Duration.standardDays(30)));
        when(search.id()).thenReturn(IN_USE_SEARCH_ID);

        when(searchDbService.streamAll()).thenReturn(Stream.of(search));

        this.searchesCleanUpJob.doRun();

        // Verify that search ids for standard views and resolved views are passed in as neverDeleteIds.
        final ArgumentCaptor<HashSet<String>> searchIdsCaptor = ArgumentCaptor.forClass((Class) Set.class);
        verify(searchDbService, times(1)).getExpiredSearches(searchIdsCaptor.capture(), any());
        assertTrue(searchIdsCaptor.getValue().contains(IN_USE_SEARCH_ID));
        assertTrue(searchIdsCaptor.getValue().contains(IN_USE_RESOLVER_SEARCH_ID));

        verify(searchDbService, never()).delete(any());
    }

    @Test
    public void testForMixedReferencedNonReferencedExpiredAndNonexpiredSearches() {
        final ViewSummaryDTO view = mock(ViewSummaryDTO.class);
        when(view.searchId()).thenReturn(IN_USE_SEARCH_ID);
        when(viewService.streamAll()).thenReturn(Stream.of(view));

        final SearchSummary search1 = mock(SearchSummary.class);
        when(search1.createdAt()).thenReturn(DateTime.now(DateTimeZone.UTC).minus(Duration.standardDays(30)));
        when(search1.id()).thenReturn(IN_USE_SEARCH_ID);

        final SearchSummary search2 = mock(SearchSummary.class);
        when(search2.createdAt()).thenReturn(DateTime.now(DateTimeZone.UTC).minus(Duration.standardHours(2)));

        final SearchSummary search3 = mock(SearchSummary.class);
        when(search3.createdAt()).thenReturn(DateTime.now(DateTimeZone.UTC).minus(Duration.standardDays(30)));
        when(search3.id()).thenReturn("This search is expired and should be deleted");

        when(searchDbService.findSummaries()).thenReturn(Stream.of(search1, search2, search3));
        when(searchDbService.getExpiredSearches(any(), any())).thenCallRealMethod();

        this.searchesCleanUpJob.doRun();

        final ArgumentCaptor<String> deletedSearchId = ArgumentCaptor.forClass(String.class);
        verify(searchDbService, times(1)).delete(deletedSearchId.capture());
        assertThat(deletedSearchId.getValue()).isEqualTo("This search is expired and should be deleted");
    }

    @Test
    public void testForEmptyViews() {
        when(viewService.streamAll()).thenReturn(Stream.empty());

        final SearchSummary search = mock(SearchSummary.class);
        when(search.createdAt()).thenReturn(DateTime.now(DateTimeZone.UTC).minus(Duration.standardDays(30)));
        when(search.id()).thenReturn("This search is expired and should be deleted");
        when(searchDbService.findSummaries()).thenReturn(Stream.of(search));
        when(searchDbService.getExpiredSearches(any(), any())).thenCallRealMethod();

        this.searchesCleanUpJob.doRun();

        final ArgumentCaptor<String> deletedSearchId = ArgumentCaptor.forClass(String.class);
        verify(searchDbService, times(1)).delete(deletedSearchId.capture());
        assertThat(deletedSearchId.getValue()).isEqualTo("This search is expired and should be deleted");
    }

    @Test
    public void testForEmptyViews2() {
        when(viewService.streamAll()).thenReturn(Stream.empty());

        when(searchDbService.getExpiredSearches(any(), any())).thenReturn(new HashSet<>(Arrays.asList("This search is expired and should be deleted")));

        this.searchesCleanUpJob.doRun();

        final ArgumentCaptor<String> deletedSearchId = ArgumentCaptor.forClass(String.class);
        verify(searchDbService, times(1)).delete(deletedSearchId.capture());
        assertThat(deletedSearchId.getValue()).isEqualTo("This search is expired and should be deleted");
    }

    private HashMap<String, ViewResolver> testViewResolvers() {
        final HashMap<String, ViewResolver> viewResolvers = new HashMap<>();
        viewResolvers.put("test-resolver", new TestViewResolver());

        return viewResolvers;
    }

    private static class TestViewResolver implements ViewResolver {
        @Override
        public Optional<ViewDTO> get(String id) {
            return Optional.empty();
        }

        @Override
        public Set<String> getSearchIds() {
            return Collections.singleton(IN_USE_RESOLVER_SEARCH_ID);
        }

        @Override
        public Set<ViewDTO> getBySearchId(String searchId) {
            return Collections.emptySet();
        }

        @Override
        public boolean canReadView(String viewId, Predicate<String> permissionTester, BiPredicate<String, String> entityPermissionsTester) {
            // Not used in this test.
            return false;
        }
    }
}
