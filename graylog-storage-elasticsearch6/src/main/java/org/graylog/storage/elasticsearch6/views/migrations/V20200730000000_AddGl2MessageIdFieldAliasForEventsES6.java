package org.graylog.storage.elasticsearch6.views.migrations;

import org.graylog.plugins.views.migrations.V20200730000000_AddGl2MessageIdFieldAliasForEvents;

import java.util.Set;

public class V20200730000000_AddGl2MessageIdFieldAliasForEventsES6 implements V20200730000000_AddGl2MessageIdFieldAliasForEvents.ElasticsearchAdapter {
    @Override
    public void addGl2MessageIdFieldAlias(Set<String> indexPrefixes) {
        throw new IllegalStateException("Field aliases are not supported for all minor versions of ES6. This should never be called.");
    }
}
