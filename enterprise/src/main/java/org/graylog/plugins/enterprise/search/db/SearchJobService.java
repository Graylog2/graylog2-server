package org.graylog.plugins.enterprise.search.db;

import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.SearchJob;

import java.util.Optional;

public interface SearchJobService {

    SearchJob create(Search query);
    Optional<SearchJob> load(String id);
    boolean delete(String id);

}
