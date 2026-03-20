/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.storage.opensearch3.views.migrations;

import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import org.graylog.plugins.views.migrations.V20200730000000_AddGl2MessageIdFieldAliasForEvents;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog2.indexer.ElasticsearchException;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch.indices.PutMappingRequest;
import org.opensearch.client.opensearch.indices.PutMappingResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.plugin.Message.FIELD_GL2_MESSAGE_ID;

public class V20200730000000_AddGl2MessageIdFieldAliasForEventsOS2 implements V20200730000000_AddGl2MessageIdFieldAliasForEvents.ElasticsearchAdapter {

    private final OfficialOpensearchClient client;

    @Inject
    public V20200730000000_AddGl2MessageIdFieldAliasForEventsOS2(OfficialOpensearchClient client) {
        this.client = client;
    }

    @Override
    public void addGl2MessageIdFieldAlias(Set<String> indexPrefixes) {

        final List<String> prefixesWithWildcard = indexPrefixes.stream().map(p -> p + "*").collect(Collectors.toList());
        PutMappingRequest request = PutMappingRequest.builder()
                .index(prefixesWithWildcard)
                .allowNoIndices(true)
                .expandWildcards(ExpandWildcard.Open, ExpandWildcard.Closed)
                .properties(ImmutableMap.of(FIELD_GL2_MESSAGE_ID, aliasMapping()))
                .build();
        PutMappingResponse response = client.sync(c -> c.indices().putMapping(request), errorMsgFor(prefixesWithWildcard));
        if (!response.acknowledged()) {
            throw new ElasticsearchException(errorMsgFor(prefixesWithWildcard) + " Opensearch failed to acknowledge.");
        }

    }

    private String errorMsgFor(List<String> prefixesWithWildcard) {
        return "Failed to add field alias " + FIELD_GL2_MESSAGE_ID + " for indices " + prefixesWithWildcard.toString() + ".";
    }

    static Property aliasMapping() {
        LinkedHashMap<String, Object> aliasMapping = new LinkedHashMap<>();
        return Property.builder()
                .alias(a -> a.path("id"))
                .build();
    }
}
