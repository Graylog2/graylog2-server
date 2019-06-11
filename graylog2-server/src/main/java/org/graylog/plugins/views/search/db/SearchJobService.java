package org.graylog.plugins.views.search.db;

import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;

import java.util.Optional;

public interface SearchJobService {

    SearchJob create(Search query, String owner);
    Optional<SearchJob> load(String id, String owner);
}
