package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;

import java.util.Set;

public interface QueryGenerator {
    Object generate(SearchJob job, Query query, Set<QueryResult> results);
}
