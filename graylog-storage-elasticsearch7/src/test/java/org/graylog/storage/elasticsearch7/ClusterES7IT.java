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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import org.graylog.storage.elasticsearch7.cat.CatApi;
import org.graylog.storage.elasticsearch7.cat.NodeResponse;
import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.ClusterIT;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Rule;

import java.util.List;
import java.util.Optional;

public class ClusterES7IT extends ClusterIT {
    @Rule
    public final ElasticsearchInstanceES7 elasticsearch = ElasticsearchInstanceES7.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Override
    protected ClusterAdapter clusterAdapter(Duration timeout) {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        return new ClusterAdapterES7(elasticsearch.elasticsearchClient(),
                timeout,
                new CatApi(objectMapper, elasticsearch.elasticsearchClient()),
                new PlainJsonApi(objectMapper, elasticsearch.elasticsearchClient()));
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

    private CatApi catApi() {
        return new CatApi(new ObjectMapperProvider().get(), elasticsearch.elasticsearchClient());
    }
}
