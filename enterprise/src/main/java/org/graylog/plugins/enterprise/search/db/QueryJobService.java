package org.graylog.plugins.enterprise.search.db;

import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryJob;

import java.util.Optional;

public interface QueryJobService {

    QueryJob create(Query query);
    Optional<QueryJob> load(String id);
    boolean delete(String id);

}
