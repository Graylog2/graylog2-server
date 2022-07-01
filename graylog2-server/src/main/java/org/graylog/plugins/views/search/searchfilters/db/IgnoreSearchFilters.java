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

import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Search filters are enterprise feature, so by default they won't be loaded.
 *
 * As long as proper enterprise module won't be loaded, this class will provide dummy/do-nothing implementations for Search Filter related functionality.
 */
public class IgnoreSearchFilters implements UsedSearchFiltersToQueryStringsMapper, SearchFiltersReFetcher {

    @Override
    public Set<String> map(final Collection<UsedSearchFilter> usedSearchFilters) {
        return Collections.emptySet();
    }

    @Override
    public List<UsedSearchFilter> reFetch(List<UsedSearchFilter> storedInSearch) {
        return storedInSearch;
    }
}
