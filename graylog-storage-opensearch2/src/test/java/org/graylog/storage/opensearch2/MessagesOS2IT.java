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
package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.indices.refresh.RefreshRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.index.IndexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.index.IndexResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.core.CountRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.core.CountResponse;
import org.graylog.shaded.opensearch2.org.opensearch.rest.RestStatus;
import org.graylog.storage.opensearch2.testing.OpenSearchInstance;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.indexer.messages.MessagesIT;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Rule;

import java.util.Map;

public class MessagesOS2IT extends MessagesIT {
    @Rule
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.create();

    @Override
    protected SearchServerInstance searchServer() {
        return this.openSearchInstance;
    }

    @Override
    protected boolean indexMessage(String index, Map<String, Object> source, String id) {
        final IndexRequest indexRequest = new IndexRequest(index)
                .source(source)
                .id(id);

        final IndexResponse result = this.openSearchInstance.openSearchClient().execute((c, requestOptions) -> c.index(indexRequest, requestOptions));

        return result.status().equals(RestStatus.CREATED);
    }
}
