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

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import jakarta.inject.Inject;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.graylog.shaded.opensearch2.org.opensearch.client.Response;
import org.graylog.shaded.opensearch2.org.opensearch.common.xcontent.json.JsonXContent;
import org.graylog.shaded.opensearch2.org.opensearch.core.action.ActionListener;
import org.graylog.shaded.opensearch2.org.opensearch.core.common.bytes.BytesReference;
import org.graylog.shaded.opensearch2.org.opensearch.core.xcontent.ToXContent;
import org.graylog.shaded.opensearch2.org.opensearch.core.xcontent.XContentBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.reindex.BulkByScrollResponse;
import org.graylog.shaded.opensearch2.org.opensearch.index.reindex.ReindexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.index.reindex.RemoteInfo;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.datanode.RemoteReindexAllowlistEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.migration.RemoteReindexIndex;
import org.graylog2.indexer.migration.RemoteReindexMigration;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.matchAllQuery;

public class RemoteReindexingMigrationAdapterOS2 implements RemoteReindexingMigrationAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteReindexingMigrationAdapterOS2.class);

    private static final int CONNECTION_ATTEMPTS = 40;
    private static final int WAIT_BETWEEN_CONNECTION_ATTEMPTS = 3;

    private final OpenSearchClient client;
    private final OkHttpClient httpClient;
    private final NodeService<DataNodeDto> nodeService;
    private final Indices indices;
    private final IndexSetRegistry indexSetRegistry;
    private final ClusterEventBus eventBus;

    private RemoteReindexMigration remoteReindexMigration = RemoteReindexMigration.builder().status(Status.STARTING).indices(List.of()).build();

    @Inject
    public RemoteReindexingMigrationAdapterOS2(final OpenSearchClient client,
                                               final OkHttpClient httpClient,
                                               final NodeService<DataNodeDto> nodeService,
                                               final Indices indices,
                                               final IndexSetRegistry indexSetRegistry,
                                               final ClusterEventBus eventBus) {
        this.client = client;
        this.httpClient = httpClient;
        this.nodeService = nodeService;
        this.indices = indices;
        this.indexSetRegistry = indexSetRegistry;
        this.eventBus = eventBus;
    }

    @Override
    public Status start(final URI uri, final String username, final String password, final List<String> indices) {
        try {
            final var toReindex = isAllIndices(indices) ? getAllIndicesFrom(uri, username, password) : indices;

            final var activeNodes = getActiveNodes();

            allowReindexingFrom(uri.getHost() + ":" + uri.getPort());

            waitForClusterRestart(activeNodes);

            reindex(uri, username, password, toReindex);
//            removeAllowlist(uri.getHost() + ":" + uri.getPort());
            return Status.STARTING;
        } catch (MalformedURLException e) {
            LOG.error("Could not start migration: {}", e.getMessage(), e);
            return Status.ERROR;
        }
    }

    @Override
    public RemoteReindexMigration status() {
        return remoteReindexMigration;
    }

    Set<String> getActiveNodes() {
        return nodeService.allActive().values().stream()
                .filter(dn -> dn.getDataNodeStatus() == DataNodeStatus.AVAILABLE) // we have to wait till the datanode is not just alive but all started properly and the indexer accepts connections
                .map(NodeDto::getNodeId).collect(Collectors.toSet());
    }

    void waitForClusterRestart(final Set<String> expectedNodes) {
        // sleeping for some time to let the cluster stop so we can wait for the restart
        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            LOG.warn("Could not sleep...");
        }

        final var retryer = RetryerBuilder.<Boolean>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(WAIT_BETWEEN_CONNECTION_ATTEMPTS, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(CONNECTION_ATTEMPTS))
                .retryIfResult(response -> !response)
                .retryIfException()
                .build();

        final Callable<Boolean> callable = () -> getActiveNodes().containsAll(expectedNodes);

        try {
            var successful = retryer.call(callable);
            if (!successful) {
                LOG.error("Cluster failed to restart after " + CONNECTION_ATTEMPTS * WAIT_BETWEEN_CONNECTION_ATTEMPTS + " seconds.");
            }
        } catch (ExecutionException | RetryException e) {
            LOG.error("Cluster failed to restart: " + e.getMessage(), e);
        }

    }

    void removeAllowlist(final String host) {
        eventBus.post(new RemoteReindexAllowlistEvent(host, RemoteReindexAllowlistEvent.ACTION.REMOVE));
    }

    void allowReindexingFrom(final String host) {
        eventBus.post(new RemoteReindexAllowlistEvent(host, RemoteReindexAllowlistEvent.ACTION.ADD));
    }

    List<String> getAllIndicesFrom(final URI uri, final String username, final String password) throws MalformedURLException {
        final var host = uri.toURL().toString();
        var url = (host.endsWith("/") ? host : host + "/") + "_cat/indices?h=index";
        try (var response = httpClient.newCall(new Request.Builder().url(url).header("Authorization", Credentials.basic(username, password)).build()).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return new BufferedReader(new StringReader(response.body().string())).lines().toList();
            } else {
                throw new RuntimeException("Could not read list of indices from " + host);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read list of indices from " + host + ", " + e.getMessage(), e);
        }
    }

    boolean isAllIndices(final List<String> indices) {
        return indices == null || indices.isEmpty() || (indices.size() == 1 && "*".equals(indices.get(0)));
    }

    abstract class ReindexActionListener<T> implements ActionListener<T> {
        private final String name;

        public ReindexActionListener(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }
    }

    void reindex(final URI uri, final String username, final String password, final List<String> indices) {
        final var remoteReindexIndices = new ArrayList<RemoteReindexIndex>();
        try (XContentBuilder builder = JsonXContent.contentBuilder().prettyPrint()) {
            BytesReference query = BytesReference.bytes(matchAllQuery().toXContent(builder, ToXContent.EMPTY_PARAMS));

            for (final var index : indices) {
                if (this.indices.exists(index)) {
                    remoteReindexIndices.add(new RemoteReindexIndex(index, Status.ERROR, "Can't migrate index " + index + ", as it already exists in the target indexer."));
                } else {
                    this.indices.create(index, indexSetRegistry.getForIndex(index).orElse(indexSetRegistry.getDefault()));

                    final ReindexRequest reindexRequest = new ReindexRequest();
                    reindexRequest
                            .setRemoteInfo(new RemoteInfo(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), query, username, password, Map.of(), RemoteInfo.DEFAULT_SOCKET_TIMEOUT, RemoteInfo.DEFAULT_CONNECT_TIMEOUT))
                            .setSourceIndices(index).setDestIndex(index);

                    final var reindexResult = client.execute((c, requestOptions) -> c.reindexAsync(reindexRequest, requestOptions, new ReindexActionListener<>(index) {
                        @Override
                        public void onResponse(final BulkByScrollResponse response) {
                            synchronized (remoteReindexMigration) {
                                final var unused = remoteReindexMigration.indices().stream().filter(f -> f.name().equals(index)).findFirst().map(i -> {
                                    final var others = remoteReindexMigration.indices().stream().filter(f -> !f.name().equals(index)).toList();
                                    remoteReindexMigration.indices().clear();
                                    remoteReindexMigration.indices().addAll(others);
                                    remoteReindexMigration.indices().add(new RemoteReindexIndex(i.name(), Status.FINISHED, i.created(), new Duration(response.getTotal()), response.getBatches()));
                                    return null;
                                });
                            }
                        }

                        @Override
                        public void onFailure(final Exception e) {
                            synchronized (remoteReindexMigration) {
                                final var unused = remoteReindexMigration.indices().stream().filter(f -> f.name().equals(index)).findFirst().map(i -> {
                                    final var others = remoteReindexMigration.indices().stream().filter(f -> !f.name().equals(index)).toList();
                                    remoteReindexMigration.indices().clear();
                                    remoteReindexMigration.indices().addAll(others);
                                    remoteReindexMigration.indices().add(new RemoteReindexIndex(i.name(), Status.ERROR, i.created(), e.getMessage()));
                                    return null;
                                });
                            }
                        }
                    }));
                    remoteReindexIndices.add(new RemoteReindexIndex(index, Status.RUNNING));
                }
            }
            remoteReindexMigration = RemoteReindexMigration.builder().status(Status.RUNNING).indices(remoteReindexIndices).build();
        } catch (IOException e) {
            LOG.error("Could not reindex data: {}", e.getMessage(), e);
            remoteReindexMigration = RemoteReindexMigration.builder().status(Status.ERROR).error("An error occurred, refer to the logs for more info: " + e.getMessage()).indices(remoteReindexIndices).build();
        }
    }
}
