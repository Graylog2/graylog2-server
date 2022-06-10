package org.graylog.plugins.views.search.searchfilters.model;

public interface QueryStringSearchFilter {
    String queryString();

    UsedSearchFilter withQueryString(String queryString);
}
