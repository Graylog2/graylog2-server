package org.graylog.plugins.views.search.engine.validation;

import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.permissions.StreamPermissions;

import java.util.Set;

public interface SearchValidation {
    Set<SearchError> validate(Search search, StreamPermissions streamPermissions);
}
