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
package org.graylog.plugins.views.search.engine.validation;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.errors.QueryError;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.errors.SearchTypeError;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.searchtypes.DataLakeSearchType;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DataLakeSearchValidator implements SearchValidator {

    @Override
    public Set<SearchError> validate(final Search search,
                                     final SearchUser searchUser) {
        //this should be either validated elsewhere or impossible
        assert search.queries() != null;
        assert !search.queries().isEmpty();

        if (containsDataLakeSearchElements(search)) {
            if (search.queries().size() > 1) {
                return wholeSearchInvalid(search, "Data Lake elements present in Search, only 1 query allowed for those type of searches");
            }
            return validate(search.queries().stream().findFirst().get(), searchUser);
        } else {
            return Set.of();
        }
    }

    @Override
    public Set<SearchError> validate(final Query query,
                                     final SearchUser searchUser) {
        if (containsDataLakeSearchElements(query)) {
            final BackendQuery backendQuery = query.query();
            if (!isDataLakeBackend(backendQuery)) {
                return Set.of(new QueryError(query, "Data Lake query must contain Data Lake Backend"));
            }
            final ImmutableSet<SearchType> searchTypes = query.searchTypes();
            if (searchTypes.size() != 1) {
                return Set.of(new QueryError(query, "Data Lake query can contain only one search type"));
            }
            final Optional<SearchType> first = searchTypes.stream().findFirst();

            final Set<String> streams = first.get().streams();
            if (streams == null || streams.size() > 1) {
                return Set.of(new SearchTypeError(query, first.get().id(),
                        "Data Lake preview can be executed on only 1 stream, search type contained more"));
            }

        }
        return Set.of();
    }

    public static boolean containsDataLakeSearchElements(final Search search) {
        return search.queries().stream().anyMatch(DataLakeSearchValidator::containsDataLakeSearchElements);
    }

    public static boolean containsDataLakeSearchElements(final Query query) {
        return isDataLakeBackend(query.query())
                || (query.searchTypes().stream().anyMatch(searchType -> searchType instanceof DataLakeSearchType));
    }

    private static boolean isDataLakeBackend(final BackendQuery backendQuery) {
        return backendQuery.type().startsWith(DataLakeSearchType.PREFIX);
    }

    private Set<SearchError> wholeSearchInvalid(final Search search, final String explanation) {
        return search.queries()
                .stream()
                .map(query -> new QueryError(query, explanation))
                .collect(Collectors.toSet());
    }
}
