package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.search.errors.SearchError;

import java.util.Collection;

public interface GeneratedQueryContext {

    void addError(SearchError error);

    Collection<SearchError> errors();
}
