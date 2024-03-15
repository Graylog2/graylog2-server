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
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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
import org.graylog2.indexer.migration.IndexerConnectionCheckResult;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.matchAllQuery;

@Singleton
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

    // TODO: this should be mongodb-persisted to ensure that any node in the cluster sees the same values. Other approach
    // would be to use regular graylog job. Then the migration ID would be the job ID
    // The migration and its status won't survive node restart. In case of graylog cluster with several nodes,
    // the migration may be started on one and another node won't deliver any status, there will be no migration
    // information available. These limitations are known and accepted for now.
    private static final Map<String, RemoteReindexMigration> JOBS = new ConcurrentHashMap<>();

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
    public RemoteReindexMigration start(final URI uri, final String username, final String password, final List<String> indices, final boolean synchronous) {
        final RemoteReindexMigration migration = new RemoteReindexMigration();
        JOBS.put(migration.id(), migration);
        try {
            migration.setIndices(collectIndices(uri, username, password, indices));
            // finish can happen very late, in async code, so we need to handle it as a callback
            // but the async nature causes problems with closed connections :-/
            // migration.setFinishCallback(() -> removeAllowlist(uri.getHost() + ":" + uri.getPort()));
            doStartMigration(uri, username, password, synchronous, migration);
        } catch (MalformedURLException e) {
            migration.error("Failed to collect indices for migration: " + e.getMessage());
        }
        return migration;
    }

    private List<RemoteReindexIndex> collectIndices(URI uri, String username, String password, List<String> indices) throws MalformedURLException {
        final List<String> toReindex = isAllIndices(indices) ? getAllIndicesFrom(uri, username, password) : indices;
        return toReindex.stream().map(indexName -> new RemoteReindexIndex(indexName, Status.NOT_STARTED)).collect(Collectors.toList());
    }

    private void doStartMigration(URI uri, String username, String password, boolean synchronous, RemoteReindexMigration migration) {
        prepareCluster(uri);
        migration.status(Status.RUNNING);
        createIndicesInNewCluster(migration);
        if (synchronous) {
            reindexSynchronously(migration, uri, username, password);
        } else {
            reindexNextAvailableAsync(migration, uri, username, password);
        }
    }

    private void createIndicesInNewCluster(RemoteReindexMigration migration) {
        migration.indices().forEach(index -> {
            if (!this.indices.exists(index.getName())) {
                this.indices.create(index.getName(), indexSetRegistry.getForIndex(index.getName()).orElse(indexSetRegistry.getDefault()));
            } else {
                LOG.info("Index {} does already exist in target indexer. Data will be migrated into existing index.", index.getName());
            }
        });
    }

    private void prepareCluster(URI uri) {
        final var activeNodes = getAllActiveNodeIDs();
        allowReindexingFrom(uri.getHost() + ":" + uri.getPort());
        waitForClusterRestart(activeNodes);
    }

    ReindexRequest createReindexRequest(final String index, final BytesReference query, URI uri, String username, String password) {
        final ReindexRequest reindexRequest = new ReindexRequest();
        reindexRequest
                .setRemoteInfo(new RemoteInfo(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), query, username, password, Map.of(), RemoteInfo.DEFAULT_SOCKET_TIMEOUT, RemoteInfo.DEFAULT_CONNECT_TIMEOUT))
                .setSourceIndices(index).setDestIndex(index);
        return reindexRequest;
    }

    private void reindexSynchronously(final RemoteReindexMigration migration, URI uri, String username, String password) {
        migration.indices().forEach(index -> reindexSync(index, uri, username, password));
        migration.finish();
    }

    private void reindexSync(RemoteReindexIndex index, URI uri, String username, String password) {
        try (XContentBuilder builder = JsonXContent.contentBuilder().prettyPrint()) {
            final BytesReference query = BytesReference.bytes(matchAllQuery().toXContent(builder, ToXContent.EMPTY_PARAMS));
            final var response = client.execute((c, requestOptions) -> c.reindex(createReindexRequest(index.getName(), query, uri, username, password), requestOptions));
            index.onFinished(new Duration(response.getTotal()), response.getBatches());
        } catch (IOException e) {
            LOG.error("Could not reindex index: {} - {}", index, e.getMessage(), e);
            index.onError(e.getMessage());
        }
    }

    @Override
    public RemoteReindexMigration status(@NotNull String migrationID) {
        return JOBS.getOrDefault(migrationID, RemoteReindexMigration.nonExistent(migrationID));
    }

    @Override
    public IndexerConnectionCheckResult checkConnection(URI uri, String username, String password) {
        try {
            final List<String> discoveredIndices = getAllIndicesFrom(uri, username, password);
            return IndexerConnectionCheckResult.success(discoveredIndices);
        } catch (MalformedURLException e) {
            return IndexerConnectionCheckResult.failure(e);
        }
    }

    private Set<String> getAllActiveNodeIDs() {
        return nodeService.allActive().values().stream()
                .filter(dn -> dn.getDataNodeStatus() == DataNodeStatus.AVAILABLE) // we have to wait till the datanode is not just alive but all started properly and the indexer accepts connections
                .map(NodeDto::getNodeId).collect(Collectors.toSet());
    }

    private void waitForClusterRestart(final Set<String> expectedNodes) {
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

        final Callable<Boolean> callable = () -> getAllActiveNodeIDs().containsAll(expectedNodes);

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
                // filtering all indices that start with "." as they indicate a system index - we don't want to reindex those
                return new BufferedReader(new StringReader(response.body().string())).lines().filter(i -> !i.startsWith(".")).toList();
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

    private void reindexNextAvailableAsync(final RemoteReindexMigration migration, URI uri, String username, String password) {
        migration.indices().stream()
                .filter(i -> i.getStatus() == Status.NOT_STARTED)
                .findFirst()
                .ifPresentOrElse(
                        index -> executeReindexAsync(migration, uri, username, password, index),
                        migration::finish); // nothing more to reindex, stop recursion here
    }


    private void executeReindexAsync(RemoteReindexMigration migration, URI uri, String username, String password, RemoteReindexIndex index) {
        final String indexName = index.getName();
        try (XContentBuilder builder = JsonXContent.contentBuilder().prettyPrint()) {
            final BytesReference query = BytesReference.bytes(matchAllQuery().toXContent(builder, ToXContent.EMPTY_PARAMS));
            LOG.info("Executing reindexAsync for " + indexName);
            client.execute((c, requestOptions) -> c.reindexAsync(createReindexRequest(indexName, query, uri, username, password), requestOptions, new ActionListener<>() {
                @Override
                public void onResponse(BulkByScrollResponse response) {
                    migration.indexByName(indexName).ifPresent(r -> r.onFinished(new Duration(response.getTotal()), response.getBatches()));
                    // one done, continue with other indices available
                    reindexNextAvailableAsync(migration, uri, username, password);
                }

                @Override
                public void onFailure(Exception e) {
                    LOG.warn("Failed to remotely reindex " + indexName, e);
                    migration.indexByName(indexName).ifPresent(r -> r.onError("Can't migrate index " + indexName + ", " + e.getMessage()));
                    // one failed, continue with other indices available
                    reindexNextAvailableAsync(migration, uri, username, password);
                }
            }));

        } catch (IOException e) {
            LOG.error("Could not reindex index: {} - {}", indexName, e.getMessage(), e);
            migration.indexByName(indexName).ifPresent(r -> r.onError("Can't migrate index " + indexName + ", " + e.getMessage()));
            // TODO: what about all other indices? Should we stop here?
        }
    }
}
