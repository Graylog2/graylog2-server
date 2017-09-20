package org.graylog.plugins.enterprise.search.engine;

import org.graylog.plugins.enterprise.search.Query;

public interface QueryGenerator {
    Object generate(Query query);
}
