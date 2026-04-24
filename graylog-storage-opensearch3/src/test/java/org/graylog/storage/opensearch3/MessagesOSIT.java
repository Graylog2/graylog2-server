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
package org.graylog.storage.opensearch3;

import org.graylog.storage.opensearch3.testing.OpenSearchInstance;
import org.graylog.storage.opensearch3.testing.OpenSearchInstanceBuilder;
import org.graylog.testing.elasticsearch.SearchInstance;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.indexer.messages.MessagesIT;
import org.opensearch.client.opensearch._types.Result;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;

import java.util.Map;

public class MessagesOSIT extends MessagesIT {
    @SearchInstance
    public final OpenSearchInstance openSearchInstance = OpenSearchInstanceBuilder.builder()
            .heapSize("2g")
            .build();

    @Override
    protected SearchServerInstance searchServer() {
        return this.openSearchInstance;
    }

    @Override
    protected boolean indexMessage(String index, Map<String, Object> source, String id) {
        final IndexRequest indexRequest = IndexRequest.of(r -> r
                .index(index)
                .document(source)
                .id(id)
        );

        final IndexResponse result;
        result = this.openSearchInstance.getOfficialOpensearchClient().sync(c ->
                c.index(indexRequest), "Error indexing document");

        return result.result() == Result.Created;
    }
}
