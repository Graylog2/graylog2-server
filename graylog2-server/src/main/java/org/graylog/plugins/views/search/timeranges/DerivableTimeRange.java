package org.graylog.plugins.views.search.timeranges;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

public interface DerivableTimeRange {
    TimeRange deriveTimeRange(Query query, SearchType searchType);
}
