package org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards;

import com.google.common.collect.ImmutableMap;
import org.bson.Document;

public class BackendQuery extends Document {
    private static final String FIELD_QUERY_STRING = "query_string";

    public BackendQuery(String queryString) {
        super(ImmutableMap.of(
                "type", "elasticsearch",
                FIELD_QUERY_STRING, queryString
        ));
    }
}
