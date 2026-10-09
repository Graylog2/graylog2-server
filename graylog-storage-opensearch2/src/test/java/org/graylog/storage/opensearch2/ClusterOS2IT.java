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
import com.github.joschi.jadconfig.util.Duration;
import org.graylog.storage.opensearch2.cat.CatApi;
import org.graylog.storage.opensearch2.cat.IndexSummaryResponse;
import org.graylog.storage.opensearch2.cat.NodeResponse;
import org.graylog.storage.opensearch2.testing.OpenSearchInstance;
import org.graylog.testing.elasticsearch.SearchInstance;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.ClusterIT;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterOS2IT extends ClusterIT {
    @SearchInstance
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.create();

    @Override
    protected SearchServerInstance searchServer() {
        return this.openSearchInstance;
    }

    @Override
    protected ClusterAdapter clusterAdapter(Duration timeout) {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        return new ClusterAdapterOS2(openSearchInstance.openSearchClient(),
                timeout,
                new CatApi(objectMapper, openSearchInstance.openSearchClient()),
                new PlainJsonApi(objectMapper, openSearchInstance.openSearchClient()));
    }

    @Override
    protected String currentNodeId() {
        return currentNode().id();
    }

    private NodeResponse currentNode() {
        final List<NodeResponse> nodes = catApi().nodes();
        return nodes.get(0);
    }

    @Override
    protected String currentNodeName() {
        return currentNode().name();
    }

    @Override
    protected String currentHostnameOrIp() {
        final NodeResponse currentNode = currentNode();
        return Optional.ofNullable(currentNode.host()).orElse(currentNode.ip());
    }

    @Test
    public void indexHealth_returns_empty_when_any_requested_index_is_missing() {
        final String index = client().createRandomIndex("cluster_it_");

        assertThat(catApi().indexHealth(List.of(index))).extracting(IndexSummaryResponse::index).containsExactly(index);
        assertThat(catApi().indexHealth(List.of(index, "cluster_it_missing"))).isEmpty();
    }

    private CatApi catApi() {
        return new CatApi(new ObjectMapperProvider().get(), openSearchInstance.openSearchClient());
    }
}
