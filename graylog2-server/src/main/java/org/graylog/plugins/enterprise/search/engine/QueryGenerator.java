package org.graylog.plugins.enterprise.search.engine;

import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryResult;
import org.graylog.plugins.enterprise.search.SearchJob;

import java.util.Set;

public interface QueryGenerator {
    Object generate(SearchJob job, Query query, Set<QueryResult> results);
}
