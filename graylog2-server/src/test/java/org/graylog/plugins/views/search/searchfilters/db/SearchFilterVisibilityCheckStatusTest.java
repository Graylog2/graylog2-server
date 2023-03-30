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
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class SearchFilterVisibilityCheckStatusTest {

    private SearchFilterVisibilityCheckStatus toTest;

    @Test
    void testAllSearchFiltersVisibleReturnsTrueOnEmptyHiddenFilters() {
        toTest = new SearchFilterVisibilityCheckStatus(Collections.emptyList());
        assertTrue(toTest.allSearchFiltersVisible());
    }

    @Test
    void testAllSearchFiltersVisibleReturnsFalseOnNonEmptyHiddenFilters() {
        toTest = new SearchFilterVisibilityCheckStatus(Collections.singletonList("There is a hidden one!"));
        assertFalse(toTest.allSearchFiltersVisible());
        assertFalse(toTest.allSearchFiltersVisible(null));
        assertFalse(toTest.allSearchFiltersVisible(Collections.emptyList()));
    }

    @Test
    void testAllSearchFiltersVisibleReturnsTrueIfHiddenFilterIsAllowed() {
        toTest = new SearchFilterVisibilityCheckStatus(Collections.singletonList("Allowed hidden one"));
        assertTrue(toTest.allSearchFiltersVisible(ImmutableList.of("Allowed hidden one", "Another allowed hidden one")));
    }

    @Test
    void testAllSearchFiltersVisibleReturnsFalseIfHiddenFilterIsForbidden() {
        toTest = new SearchFilterVisibilityCheckStatus(ImmutableList.of("Allowed hidden one", "Forbidden hidden one"));
        assertFalse(toTest.allSearchFiltersVisible(ImmutableList.of("Allowed hidden one", "Another allowed hidden one")));
    }
}
