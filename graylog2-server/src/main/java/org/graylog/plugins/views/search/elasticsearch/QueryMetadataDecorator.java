package org.graylog.plugins.views.search.elasticsearch;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.Search;

public interface QueryMetadataDecorator {
    QueryMetadata decorate(Search search, Query query, QueryMetadata queryMetadata);
}
