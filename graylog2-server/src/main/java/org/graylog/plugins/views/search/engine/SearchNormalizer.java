package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;

public interface SearchNormalizer {
    Search normalize(Search search, SearchUser searchUser, ExecutionState executionState);
}
