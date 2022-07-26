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

import java.util.Collections;
import java.util.List;

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

    public String toMessage() {
        if (!allSearchFiltersVisible()) {
            return "Search cannot be saved, as it contains Search Filters which you are not privileged to view : " + hiddenSearchFiltersIDs.toString();
        } else {
            return "Search can be created with provided list of Search Filters";
        }
    }
}
