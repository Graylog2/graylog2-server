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
package org.graylog.datanode.integration;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.github.joschi.jadconfig.util.Duration;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.graylog.datanode.testinfra.DatanodeContainerizedBackend;
import org.graylog.datanode.testinfra.DatanodeTestExtension;
import org.graylog.storage.opensearch3.ClusterAdapterOS;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.storage.opensearch3.OfficialOpensearchClientProvider;
import org.graylog2.cluster.nodes.opensearch.OpensearchNode;
import org.graylog2.cluster.nodes.opensearch.OpensearchNodesProvider;
import org.graylog2.configuration.ElasticsearchClientConfiguration;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.opensearch.OpensearchClusterResource;
import org.graylog2.security.jwt.IndexerJwtAuthToken;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Integration test for {@link OpensearchClusterResource}, backed by a real, containerized datanode.
 *
 * The datanode automatically starts and manages its own embedded OpenSearch instance, so this test only
 * needs to wire the production {@link Cluster}/{@link ClusterAdapterOS} stack to the datanode's exposed
 * OpenSearch REST port to exercise the resource end-to-end against a real cluster.
 */
@ExtendWith(DatanodeTestExtension.class)
class OpensearchClusterResourceIntegrationIT {

    private final DatanodeContainerizedBackend backend;

    private OfficialOpensearchClient officialOpensearchClient;
    private ScheduledExecutorService scheduler;
    private OpensearchClusterResource resource;

    OpensearchClusterResourceIntegrationIT(DatanodeContainerizedBackend backend) {
        this.backend = backend;
    }

    @BeforeEach
    void setUp() {
        officialOpensearchClient = buildOfficialOpensearchClient();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "opensearch-cluster-resource-it"));

        final ClusterAdapterOS clusterAdapter = new ClusterAdapterOS(officialOpensearchClient, Duration.seconds(60));
        final Cluster cluster = new Cluster(mock(IndexSetRegistry.class), scheduler, Duration.seconds(60), clusterAdapter);
        resource = new OpensearchClusterResource(new OpensearchNodesProvider(cluster));
    }

    @AfterEach
    void tearDown() {
        if (officialOpensearchClient != null) {
            officialOpensearchClient.close();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Test
    void listNodesReturnsRunningDatanodeAsOpensearchNode() throws ExecutionException, RetryException {
        final PageListResponse<OpensearchNode> response = waitForNonEmptyNodeList();

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.elements()).hasSize(1);

        final OpensearchNode node = response.elements().get(0);
        assertThat(node.name()).isEqualTo(backend.getNodeName());
        assertThat(node.version()).isNotBlank();
        assertThat(node.roles()).isNotEmpty();
    }

    @Test
    void listNodesFiltersBySearchQuery() throws ExecutionException, RetryException, QueryNodeException, IOException {
        waitForNonEmptyNodeList();

        final PageListResponse<OpensearchNode> match = resource.listNodes(1, 50,
                "name:" + backend.getNodeName(), "name", SortOrder.ASCENDING);
        assertThat(match.elements()).hasSize(1);

        final PageListResponse<OpensearchNode> noMatch = resource.listNodes(1, 50,
                "name:does-not-exist", "name", SortOrder.ASCENDING);
        assertThat(noMatch.elements()).isEmpty();
        assertThat(noMatch.total()).isZero();
    }

    private PageListResponse<OpensearchNode> waitForNonEmptyNodeList() throws ExecutionException, RetryException {
        final Retryer<PageListResponse<OpensearchNode>> retryer = RetryerBuilder.<PageListResponse<OpensearchNode>>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(30))
                .retryIfResult(result -> result.elements().isEmpty())
                .retryIfException()
                .build();

        return retryer.call(() -> resource.listNodes(1, 50, "", "name", SortOrder.ASCENDING));
    }

    private OfficialOpensearchClient buildOfficialOpensearchClient() {
        return new OfficialOpensearchClientProvider(
                List.of(URI.create("http://localhost:" + backend.getOpensearchRestPort())),
                IndexerJwtAuthToken.disabled(),
                new BasicCredentialsProvider(),
                buildClientConfiguration(),
                new ObjectMapperProvider().get(),
                null
        ).get();
    }

    private ElasticsearchClientConfiguration buildClientConfiguration() {
        final ElasticsearchClientConfiguration config = new ElasticsearchClientConfiguration();
        final Map<String, String> properties = Map.of(
                "elasticsearch_connect_timeout", "60s",
                "elasticsearch_socket_timeout", "60s",
                "elasticsearch_max_total_connections", "1",
                "elasticsearch_max_total_connections_per_route", "1",
                "elasticsearch_use_expect_continue", "false"
        );
        try {
            new JadConfig(new InMemoryRepository(properties), config).process();
        } catch (RepositoryException | ValidationException e) {
            throw new RuntimeException(e);
        }
        return config;
    }
}
