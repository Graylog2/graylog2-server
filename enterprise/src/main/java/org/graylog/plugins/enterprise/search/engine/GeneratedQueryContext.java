package org.graylog.plugins.enterprise.search.engine;

import org.graylog.plugins.enterprise.search.errors.SearchError;

import java.util.Collection;

public interface GeneratedQueryContext {

    void addError(SearchError error);

    Collection<SearchError> errors();
}
