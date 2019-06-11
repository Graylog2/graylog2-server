package org.graylog.plugins.enterprise.search.elasticsearch;

import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryMetadata;
import org.graylog.plugins.enterprise.search.Search;

public interface QueryMetadataDecorator {
    QueryMetadata decorate(Search search, Query query, QueryMetadata queryMetadata);
}
