package org.graylog.plugins.enterprise.search.db;

import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.SearchJob;

import java.util.Optional;

public interface SearchJobService {

    SearchJob create(Search query, String owner);
    Optional<SearchJob> load(String id, String owner);
}
