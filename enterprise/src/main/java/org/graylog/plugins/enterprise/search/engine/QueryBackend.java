package org.graylog.plugins.enterprise.search.engine;

import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryResult;

public interface QueryBackend {
    Object generate(Query query);

    QueryResult run(Query originalQuery, Object generatedQuery);
}
