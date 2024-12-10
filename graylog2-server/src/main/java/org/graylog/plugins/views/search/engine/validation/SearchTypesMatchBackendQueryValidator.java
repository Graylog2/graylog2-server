package org.graylog.plugins.views.search.engine.validation;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.errors.SearchTypeError;
import org.graylog.plugins.views.search.permissions.SearchUser;

import java.util.Set;
import java.util.stream.Collectors;

public class SearchTypesMatchBackendQueryValidator implements SearchValidator {

    public static final String INVALID_SEARCH_TYPE_FOR_GIVEN_QUERY_TYPE_MSG = "Invalid search type for given query type";

    @Override
    public Set<SearchError> validate(final Search search,
                                     final SearchUser searchUser) {
        return search.queries()
                .stream()
                .flatMap(query -> validate(query, searchUser).stream())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<SearchError> validate(final Query query,
                                     final SearchUser searchUser) {
        return query.searchTypes().stream()
                .filter(searchType -> query.query()
                        .supportedSearchTypes()
                        .stream()
                        .anyMatch(supportedType -> !supportedType.isAssignableFrom(searchType.getClass())))
                .map(searchType -> new SearchTypeError(query, searchType.id(), INVALID_SEARCH_TYPE_FOR_GIVEN_QUERY_TYPE_MSG))
                .collect(Collectors.toSet());
    }
}
