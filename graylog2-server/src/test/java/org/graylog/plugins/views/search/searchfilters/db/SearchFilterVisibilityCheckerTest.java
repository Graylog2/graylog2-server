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

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Search;
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
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class SearchFilterVisibilityCheckerTest {

    @Mock
    private Search search;
    private SearchFilterVisibilityChecker toTest;

    @BeforeEach
    void setUp() {
        toTest = new SearchFilterVisibilityChecker();
    }


    @Test
    void testSuccessfulCheckOnSearchWithoutReferencedFilters() {
        doReturn(ImmutableSet.of()).when(search).getReferencedSearchFiltersIds();
        final Predicate<String> alwaysTruePredicate = filterId -> true;
        final SearchFilterVisibilityCheckStatus result = toTest.checkSearchFilterVisibility(alwaysTruePredicate, search);
        assertTrue(result.allSearchFiltersVisible());
    }

    @Test
    void testSuccessfulCheckOnSearchWithOnlyVisibleReferencedFilters() {
        doReturn(ImmutableSet.of("id1", "id2", "id3")).when(search).getReferencedSearchFiltersIds();
        final Predicate<String> alwaysTruePredicate = filterId -> true;
        final SearchFilterVisibilityCheckStatus result = toTest.checkSearchFilterVisibility(alwaysTruePredicate, search);
        assertTrue(result.allSearchFiltersVisible());
    }

    @Test
    void testSingleInvisibleFilterMakesCheckFail() {
        doReturn(ImmutableSet.of("id1", "id2", "id3", "hidden")).when(search).getReferencedSearchFiltersIds();
        final Predicate<String> readPermissionPredicate = filterId -> !"hidden".equals(filterId);
        final SearchFilterVisibilityCheckStatus result = toTest.checkSearchFilterVisibility(readPermissionPredicate, search);
        assertFalse(result.allSearchFiltersVisible());
        assertEquals(Collections.singletonList("hidden"), result.getHiddenSearchFiltersIDs());
    }
}
