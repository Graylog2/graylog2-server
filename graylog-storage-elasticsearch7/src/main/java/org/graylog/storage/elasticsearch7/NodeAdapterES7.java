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
package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.core.MainResponse;
import org.graylog2.indexer.cluster.NodeAdapter;

import javax.inject.Inject;
import java.util.Optional;

public class NodeAdapterES7 implements NodeAdapter {
    private final ElasticsearchClient client;

    @Inject
    public NodeAdapterES7(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public Optional<String> version() {
        final MainResponse result = client.execute(RestHighLevelClient::info,
                "Unable to retrieve Elasticsearch version from node");
        return Optional.of(result.getVersion().getNumber());
    }
}
