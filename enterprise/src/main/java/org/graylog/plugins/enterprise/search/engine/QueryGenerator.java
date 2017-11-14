package org.graylog.plugins.enterprise.search.engine;

import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.SearchJob;

public interface QueryGenerator {
    Object generate(SearchJob job, Query query);
}
