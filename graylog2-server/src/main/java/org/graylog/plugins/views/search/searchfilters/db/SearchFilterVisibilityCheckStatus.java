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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SearchFilterVisibilityCheckStatus {

    private final List<String> hiddenSearchFiltersIDs;

    public SearchFilterVisibilityCheckStatus() {
        this.hiddenSearchFiltersIDs = Collections.emptyList();
    }

    public SearchFilterVisibilityCheckStatus(final List<String> hiddenSearchFiltersIDs) {
        this.hiddenSearchFiltersIDs = hiddenSearchFiltersIDs;
    }

    public List<String> getHiddenSearchFiltersIDs() {
        return hiddenSearchFiltersIDs;
    }

    public boolean allSearchFiltersVisible() {
        return hiddenSearchFiltersIDs.isEmpty();
    }

    public boolean allSearchFiltersVisible(final Collection<String> allowedHiddenSearchFilters) {
        return hiddenSearchFiltersIDs.isEmpty() || (allowedHiddenSearchFilters != null && allowedHiddenSearchFilters.containsAll(hiddenSearchFiltersIDs));
    }

    public String toMessage() {
        if (!allSearchFiltersVisible()) {
            return "Search cannot be saved, as it contains Search Filters which you are not privileged to view : " + hiddenSearchFiltersIDs.toString();
        } else {
            return "Search can be created with provided list of Search Filters";
        }
    }

    public String toMessage(final Collection<String> allowedHiddenSearchFilters) {
        if (!allSearchFiltersVisible(allowedHiddenSearchFilters)) {
            return "Search cannot be saved, as it contains Search Filters which you are not privileged to view : " +
                    hiddenSearchFiltersIDs.stream()
                            .filter(f -> !allowedHiddenSearchFilters.contains(f))
                            .collect(Collectors.toList());
        } else {
            return "Search can be created with provided list of Search Filters";
        }
    }
}
