package org.graylog.plugins.views.search.elasticsearch;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;

import java.util.Set;

public interface ESQueryDecorator {
    String decorate(String queryString, SearchJob job, Query query, Set<QueryResult> results);
}
