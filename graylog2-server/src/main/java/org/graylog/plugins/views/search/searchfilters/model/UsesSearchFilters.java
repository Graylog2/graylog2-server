package org.graylog.plugins.views.search.searchfilters.model;

import java.util.List;

public interface UsesSearchFilters {

    List<UsedSearchFilter> filters();
}
