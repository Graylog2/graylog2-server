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
import java.util.HashSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchesCleanUpJobTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ViewSummaryService viewService;

    @Mock
    private SearchDbService searchDbService;

    private SearchesCleanUpJob searchesCleanUpJob;

    @Before
    public void setup() {
        this.searchesCleanUpJob = new SearchesCleanUpJob(viewService, searchDbService, Duration.standardDays(4));
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
        final String searchId = "This search is in use";
        final ViewSummaryDTO view = mock(ViewSummaryDTO.class);
        when(view.searchId()).thenReturn(searchId);
        when(viewService.streamAll()).thenReturn(Stream.of(view));

        final Search search = mock(Search.class);
        when(search.createdAt()).thenReturn(DateTime.now(DateTimeZone.UTC).minus(Duration.standardDays(30)));
        when(search.id()).thenReturn(searchId);

        when(searchDbService.streamAll()).thenReturn(Stream.of(search));

        this.searchesCleanUpJob.doRun();

        verify(searchDbService, never()).delete(any());
    }

    @Test
    public void testForMixedReferencedNonReferencedExpiredAndNonexpiredSearches() {
        final String searchId = "This search is in use";
        final ViewSummaryDTO view = mock(ViewSummaryDTO.class);
        when(view.searchId()).thenReturn(searchId);
        when(viewService.streamAll()).thenReturn(Stream.of(view));

        final SearchSummary search1 = mock(SearchSummary.class);
        when(search1.createdAt()).thenReturn(DateTime.now(DateTimeZone.UTC).minus(Duration.standardDays(30)));
        when(search1.id()).thenReturn(searchId);

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
}
