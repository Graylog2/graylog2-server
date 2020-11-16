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
package org.graylog.storage.elasticsearch7.views.export;

import org.graylog.plugins.views.search.export.ExportException;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RequestOptions;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.storage.elasticsearch7.ThrowingBiFunction;
import org.graylog2.indexer.ElasticsearchException;

import javax.inject.Inject;
import java.io.IOException;

public class ExportClient {
    private final ElasticsearchClient client;

    @Inject
    public ExportClient(ElasticsearchClient client) {
        this.client = client;
    }

    public SearchResponse search(SearchRequest request, String errorMessage) {
        try {
            return this.client.search(request, errorMessage);
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
