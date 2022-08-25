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
package org.graylog.plugins.views.search.searchfilters;

import org.graylog.plugins.views.search.searchfilters.model.ReferencedSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.UsesSearchFilters;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ReferencedSearchFiltersRetriever {

    public Set<String> getReferencedSearchFiltersIds(final Collection<UsesSearchFilters> searchFiltersOwners) {
        return searchFiltersOwners
                .stream()
                .map(UsesSearchFilters::filters)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(usedSearchFilter -> usedSearchFilter instanceof ReferencedSearchFilter)
                .map(usedSearchFilter -> (ReferencedSearchFilter) usedSearchFilter)
                .map(ReferencedSearchFilter::id)
                .collect(Collectors.toSet());
    }
}
