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
package org.graylog.storage.opensearch2.views.export;

import jakarta.inject.Inject;
import org.graylog.plugins.views.search.export.ExportException;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.RequestOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.shaded.opensearch2.org.opensearch.core.action.ShardOperationFailedException;
import org.graylog.storage.opensearch2.OpenSearchClient;
import org.graylog.storage.opensearch2.ThrowingBiFunction;
import org.graylog2.indexer.ElasticsearchException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ExportClient {
    private final OpenSearchClient client;

    @Inject
    public ExportClient(OpenSearchClient client) {
        this.client = client;
    }

    public SearchResponse search(SearchRequest request, String errorMessage) {
        try {
            final SearchResponse response = this.client.search(request, errorMessage);
            if (response.getFailedShards() > 0) {
                final List<String> errors = Arrays.stream(response.getShardFailures())
                        .map(ShardOperationFailedException::getCause)
                        .map(Throwable::getMessage)
                        .distinct()
                        .toList();
                throw new ElasticsearchException("Unable to perform export query: ", errors);
            }
            return response;
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    private ExportException wrapException(Exception e) {
        return new ExportException("Unable to complete export: ", new ElasticsearchException(e));
    }

    public SearchResponse singleSearch(SearchRequest request, String errorMessage) {
        try {
            return this.client.singleSearch(request, errorMessage);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    public <R> R execute(ThrowingBiFunction<RestHighLevelClient, RequestOptions, R, IOException> fn, String errorMessage) {
        try {
            return this.client.execute(fn, errorMessage);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }
}
