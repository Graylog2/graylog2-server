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
package org.graylog.plugins.views.search.searchfilters.db;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.searchfilters.model.InlineQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.ReferencedSearchFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SearchFilterVisibilityCheckerTest {

    @Mock
    private Search search;

    private SearchFilterVisibilityChecker toTest;
    private final Predicate<String> alwaysFalsePredicate = filterId -> false;

    @BeforeEach
    void setUp() {
        toTest = new SearchFilterVisibilityChecker();
    }

    @Test
    void testSuccessfulCheckOnSearchWithoutQueries() {
        doReturn(ImmutableSet.of()).when(search).queries();
        final SearchFilterVisibilityCheckStatus result = toTest.checkSearchFilterVisibility(alwaysFalsePredicate, search);
        assertTrue(result.allSearchFiltersVisible());
    }

    @Test
    void testSuccessfulCheckOnSearchWithQueriesWithoutFilters() {
        Query query1 = mock(Query.class);
        Query query2 = mock(Query.class);
        doReturn(ImmutableSet.of(query1, query2)).when(search).queries();
        final SearchFilterVisibilityCheckStatus result = toTest.checkSearchFilterVisibility(alwaysFalsePredicate, search);
        assertTrue(result.allSearchFiltersVisible());
    }

    @Test
    void testSuccessfulCheckOnSearchWithQueriesWithOnlyVisibleFilters() {
        Query query1 = mock(Query.class);
        Query query2 = mock(Query.class);
        doReturn(ImmutableList.of(mock(ReferencedSearchFilter.class), mock(ReferencedSearchFilter.class))).when(query1).filters();
        doReturn(ImmutableList.of(mock(ReferencedSearchFilter.class))).when(query2).filters();
        doReturn(ImmutableSet.of(query1, query2)).when(search).queries();
        final Predicate<String> alwaysTruePredicate = filterId -> true;
        final SearchFilterVisibilityCheckStatus result = toTest.checkSearchFilterVisibility(alwaysTruePredicate, search);
        assertTrue(result.allSearchFiltersVisible());
    }

    @Test
    void testSingleInvisibleFilterMakesCheckFail() {
        Query query1 = mock(Query.class);
        Query query2 = mock(Query.class);
        ReferencedSearchFilter hiddenSearchFilter = mock(ReferencedSearchFilter.class);
        doReturn("hidden").when(hiddenSearchFilter).id();
        doReturn(ImmutableList.of(mock(ReferencedSearchFilter.class), mock(ReferencedSearchFilter.class), hiddenSearchFilter)).when(query1).filters();
        doReturn(ImmutableList.of(mock(ReferencedSearchFilter.class))).when(query2).filters();
        doReturn(ImmutableSet.of(query1, query2)).when(search).queries();
        final Predicate<String> readPermissionPredicate = filterId -> !"hidden".equals(filterId);
        final SearchFilterVisibilityCheckStatus result = toTest.checkSearchFilterVisibility(readPermissionPredicate, search);

        assertFalse(result.allSearchFiltersVisible());
        assertEquals(Collections.singletonList("hidden"), result.getHiddenSearchFiltersIDs());
    }

    @Test
    void testWorksOnlyWithReferencedSearchFilters() {
        Query query1 = mock(Query.class);
        Query query2 = mock(Query.class);
        doReturn(ImmutableList.of(mock(InlineQueryStringSearchFilter.class), mock(InlineQueryStringSearchFilter.class))).when(query1).filters();
        doReturn(ImmutableList.of(mock(ReferencedSearchFilter.class))).when(query2).filters();
        doReturn(ImmutableSet.of(query1, query2)).when(search).queries();
        final Predicate<String> predicateMock = mock(Predicate.class);
        toTest.checkSearchFilterVisibility(predicateMock, search);

        verify(predicateMock, times(1)).test(any());//there is only 1 ReferencedSearchFilter

    }

}
