package org.graylog.plugins.enterprise.search.elasticsearch;

import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryResult;
import org.graylog.plugins.enterprise.search.SearchJob;

import java.util.Set;

public interface ESQueryDecorator {
    String decorate(String queryString, SearchJob job, Query query, Set<QueryResult> results);
}
