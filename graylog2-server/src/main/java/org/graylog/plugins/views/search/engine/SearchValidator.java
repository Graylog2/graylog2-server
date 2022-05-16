package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.errors.SearchTypeError;
import org.graylog.plugins.views.search.permissions.StreamPermissions;

import java.util.Optional;

public interface SearchValidator {
    Optional<SearchTypeError> validateSearchType(Query query, SearchType searchType, SearchConfig searchConfig);

    void validateQueryTimeRange(Query query, SearchConfig config);

    void validate(Search search, StreamPermissions streamPermissions);
}
