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
package org.graylog.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.storage.elasticsearch7.CountsAdapterES7;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.storage.elasticsearch7.IndicesAdapterES7;
import org.graylog.storage.elasticsearch7.NodeAdapterES7;
import org.graylog.storage.elasticsearch7.cat.CatApi;
import org.graylog.storage.elasticsearch7.cluster.ClusterStateApi;
import org.graylog.storage.elasticsearch7.stats.StatsApi;
import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.storage.opensearch2.CountsAdapterOS2;
import org.graylog.storage.opensearch2.IndicesAdapterOS2;
import org.graylog.storage.opensearch2.NodeAdapterOS2;
import org.graylog.storage.opensearch2.OpenSearchClient;
import org.graylog.storage.opensearch2.testing.OpenSearchInstance;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.elasticsearch.ContainerMatrixElasticsearchBaseTest;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

public abstract class ContainerMatrixElasticsearchITBaseTest extends ContainerMatrixElasticsearchBaseTest {
    public ContainerMatrixElasticsearchITBaseTest(SearchServerInstance elasticsearch) {
        super(elasticsearch);
    }

    protected boolean isOpenSearch2() {
        return elasticsearch().searchServer().equals(SearchServer.OS2);
    }

    protected boolean isElasticsearch7() {
        return elasticsearch().searchServer().equals(SearchServer.ES7);
    }

    private ElasticsearchClient elasticsearchClient() {
        return ((ElasticsearchInstanceES7) elasticsearch()).elasticsearchClient();
    }

    private OpenSearchClient openSearchClient() {
        return ((OpenSearchInstance) elasticsearch()).openSearchClient();
    }

    protected CountsAdapter countsAdapter() {
        if (isOpenSearch2()) {
            return new CountsAdapterOS2(openSearchClient());
        } else if (isElasticsearch7()) {
            return new CountsAdapterES7(elasticsearchClient());
        } else {
            throw new RuntimeException("Search server client not supported");
        }
    }

    protected IndicesAdapter indicesAdapter() {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        if (isOpenSearch2()) {
            final OpenSearchClient client = openSearchClient();
            return new IndicesAdapterOS2(client,
                    new org.graylog.storage.opensearch2.stats.StatsApi(objectMapper, client),
                    new org.graylog.storage.opensearch2.cat.CatApi(objectMapper, client),
                    new org.graylog.storage.opensearch2.cluster.ClusterStateApi(objectMapper, client)
            );
        } else if (isElasticsearch7()) {
            final ElasticsearchClient client = elasticsearchClient();
            return new IndicesAdapterES7(
                    client,
                    new StatsApi(objectMapper, client),
                    new CatApi(objectMapper, client),
                    new ClusterStateApi(objectMapper, client)
            );
        } else {
            throw new RuntimeException("Search server client not supported");
        }
    }

    protected NodeAdapter createNodeAdapter() {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        if (isOpenSearch2()) {
            return new NodeAdapterOS2(openSearchClient(), objectMapper);
        } else if (isElasticsearch7()) {
            return new NodeAdapterES7(elasticsearchClient(), objectMapper);
        } else {
            throw new RuntimeException("Search server client not supported");
        }
    }
}
