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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterGetSettingsRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.tasks.GetTaskRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.tasks.GetTaskResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.tasks.TaskSubmissionResponse;
import org.graylog.shaded.opensearch2.org.opensearch.common.xcontent.json.JsonXContent;
import org.graylog.shaded.opensearch2.org.opensearch.core.common.bytes.BytesReference;
import org.graylog.shaded.opensearch2.org.opensearch.core.xcontent.ToXContent;
import org.graylog.shaded.opensearch2.org.opensearch.core.xcontent.XContentBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.reindex.ReindexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.index.reindex.RemoteInfo;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.datanode.RemoteReindexAllowlistEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.datanode.RemoteReindexRequest;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.migration.IndexerConnectionCheckResult;
import org.graylog2.indexer.migration.LogEntry;
import org.graylog2.indexer.migration.LogLevel;
import org.graylog2.indexer.migration.RemoteReindexIndex;
import org.graylog2.indexer.migration.RemoteReindexMigration;
import org.graylog2.indexer.migration.TaskStatus;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.matchAllQuery;

@Singleton
public class RemoteReindexingMigrationAdapterOS2 implements RemoteReindexingMigrationAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteReindexingMigrationAdapterOS2.class);

    private static final int CONNECTION_ATTEMPTS = 40;
    private static final int WAIT_BETWEEN_CONNECTION_ATTEMPTS = 3;
    public static final int TASK_UPDATE_INTERVAL_MILLIS = 1000;

    private final OpenSearchClient client;
    private final OkHttpClient httpClient;
    private final NodeService<DataNodeDto> nodeService;
    private final Indices indices;
    private final IndexSetRegistry indexSetRegistry;
    private final ClusterEventBus eventBus;
    private final ObjectMapper objectMapper;

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
                                               final ClusterEventBus eventBus,
                                               final ObjectMapper objectMapper) {
        this.client = client;
        this.httpClient = httpClient;
        this.nodeService = nodeService;
        this.indices = indices;
        this.indexSetRegistry = indexSetRegistry;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
    }

    @Override
    public RemoteReindexMigration start(RemoteReindexRequest request) {
        final RemoteReindexMigration migration = new RemoteReindexMigration();
        JOBS.put(migration.id(), migration);
        try {
            migration.setIndices(collectIndices(request));
            doStartMigration(migration, request);
        } catch (MalformedURLException e) {
            migration.error("Failed to collect indices for migration: " + e.getMessage());
        }
        return migration;
    }

    private List<RemoteReindexIndex> collectIndices(RemoteReindexRequest request) throws MalformedURLException {
        final List<String> toReindex = isAllIndices(request.indices()) ? getAllIndicesFrom(request.uri(), request.username(), request.password()) : request.indices();
        return toReindex.stream().map(indexName -> new RemoteReindexIndex(indexName, Status.NOT_STARTED)).collect(Collectors.toList());
    }

    private void doStartMigration(RemoteReindexMigration migration, RemoteReindexRequest request) {
        try {
            prepareCluster(request.uri());
            createIndicesInNewCluster(migration);
            startAsyncTasks(migration, request);
        } catch (Exception e) {
            LOG.error("Failed to start remote reindex migration", e);
            migration.error(e.getMessage());
        }
    }

    private void createIndicesInNewCluster(RemoteReindexMigration migration) {
        migration.indices().forEach(index -> {
            if (!this.indices.exists(index.getName())) {
                this.indices.create(index.getName(), indexSetRegistry.getForIndex(index.getName()).orElse(indexSetRegistry.getDefault()));
            } else {
                logInfo(migration, String.format(Locale.ROOT, "Index %s does already exist in target indexer. Data will be migrated into existing index.", index.getName()));
            }
        });
    }

    private void prepareCluster(URI uri) {
        final var activeNodes = getAllActiveNodeIDs();
        final String reindexSourceAddress = uri.getHost() + ":" + uri.getPort();
        try {
            verifyRemoteReindexAllowlistSetting(reindexSourceAddress);
        } catch (RemoteReindexNotAllowedException e) {
            // this is expected state for fresh datanode cluster - there is no value configured in the reindex.remote.allowlist
            // we have to add it to the configuration and wait till the whole cluster restarts
            allowReindexingFrom(reindexSourceAddress);
            waitForClusterRestart(activeNodes);
        }

        // verify again, just to be sure that all the configuration is in place and vali
        verifyRemoteReindexAllowlistSetting(reindexSourceAddress);
    }

    ReindexRequest createReindexRequest(final String index, final BytesReference query, URI uri, String username, String password) {
        final ReindexRequest reindexRequest = new ReindexRequest();
        reindexRequest
                .setRemoteInfo(new RemoteInfo(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), query, username, password, Map.of(), RemoteInfo.DEFAULT_SOCKET_TIMEOUT, RemoteInfo.DEFAULT_CONNECT_TIMEOUT))
                .setSourceIndices(index).setDestIndex(index);

        return reindexRequest;
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
        } catch (Exception e) {
            return IndexerConnectionCheckResult.failure(e);
        }
    }

    public void verifyRemoteReindexAllowlistSetting(String reindexSourceAddress) throws RemoteReindexNotAllowedException {
        final String allowlistSetttingValue = client.execute((restHighLevelClient, requestOptions) -> {
            final ClusterGetSettingsRequest request = new ClusterGetSettingsRequest();
            request.includeDefaults(true);
            final ClusterGetSettingsResponse settings = restHighLevelClient.cluster().getSettings(request, requestOptions);
            return settings.getSetting("reindex.remote.allowlist");
        });

        // the value is not proper json, just something like [localhost:9201]. It should be safe to simply use String.contains,
        // but there is maybe a chance for mismatches and then we'd have to parse the value
        final boolean isRemoteReindexAllowed = !allowlistSetttingValue.contains(reindexSourceAddress);
        if (isRemoteReindexAllowed) {
            final String message = "Failed to configure reindex.remote.allowlist setting in the datanode cluster. Current setting value: " + allowlistSetttingValue;
            LOG.error(message);
            throw new RemoteReindexNotAllowedException(message);
        }
    }

    private Set<String> getAllActiveNodeIDs() {
        return nodeService.allActive().values().stream()
                .filter(dn -> dn.getDataNodeStatus() == DataNodeStatus.AVAILABLE) // we have to wait till the datanode is not just alive but all started properly and the indexer accepts connections
                .map(NodeDto::getNodeId).collect(Collectors.toSet());
    }

    private void waitForClusterRestart(final Set<String> expectedNodes) {
        // We are currently unable to detect that datanodes stopped and are starting again. We just hope that
        // these 10 seconds give them enough time and they will restart the opensearch process in the background
        // after 10s we'll wait till all the previously known nodes are up and healthy again.
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
                final String message = "Cluster failed to restart after " + CONNECTION_ATTEMPTS * WAIT_BETWEEN_CONNECTION_ATTEMPTS + " seconds.";
                LOG.error(message);
                throw new IllegalStateException(message);
            }
        } catch (ExecutionException | RetryException e) {
            final String message = "Cluster failed to restart: " + e.getMessage();
            LOG.error(message, e);
            throw new RuntimeException(message);
        }
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

    private void startAsyncTasks(RemoteReindexMigration migration, RemoteReindexRequest request) {
        final int threadsCount = Math.max(1, Math.min(request.threadsCount(), migration.indices().size()));
        final ExecutorService executorService = Executors.newFixedThreadPool(threadsCount, new ThreadFactoryBuilder()
                .setNameFormat("remote-reindex-migration-backend-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler(new Tools.LogUncaughtExceptionHandler(LOG))
                .build());

        migration.indices().stream()
                .filter(i -> i.getStatus() == Status.NOT_STARTED)
                .forEach(index -> executorService.submit(() -> executeReindexAsync(migration, request.uri(), request.username(), request.password(), index)));
    }

    private void executeReindexAsync(RemoteReindexMigration migration, URI uri, String username, String password, RemoteReindexIndex index) {
        final String indexName = index.getName();
        try (XContentBuilder builder = JsonXContent.contentBuilder().prettyPrint()) {
            final BytesReference query = BytesReference.bytes(matchAllQuery().toXContent(builder, ToXContent.EMPTY_PARAMS));
            logInfo(migration, "Executing async reindex for " + indexName);
            final TaskSubmissionResponse task = client.execute((c, requestOptions) -> c.submitReindexTask(createReindexRequest(indexName, query, uri, username, password), requestOptions));
            migration.indexByName(indexName).ifPresent(i -> i.setTask(task.getTask()));

        } catch (IOException e) {
            final String message = "Could not reindex index: " + indexName + " - " + e.getMessage();
            logError(migration, message, e);
            migration.indexByName(indexName).ifPresent(r -> r.onError(duration, message));
        }
        waitForTaskCompleted(migration, index);
    }


    private void logInfo(RemoteReindexMigration migration, String message) {
        LOG.info(message);
        migration.log(new LogEntry(DateTime.now(DateTimeZone.UTC), LogLevel.INFO, message));
    }

    private void logError(RemoteReindexMigration migration, String message, Exception error) {
        if (error != null) {
            LOG.error(message, error);
        } else {
            LOG.error(message);
        }
        migration.log(new LogEntry(DateTime.now(DateTimeZone.UTC), LogLevel.ERROR, message));
    }

    private void waitForTaskCompleted(RemoteReindexMigration migration, RemoteReindexIndex index) {
        while (index.getStatus() != Status.FINISHED && index.getStatus() != Status.ERROR) {
            updateTaskStatus(migration, index);
            sleep();
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(TASK_UPDATE_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateTaskStatus(RemoteReindexMigration migration, RemoteReindexIndex index) {
        final String[] parts = index.getTaskID().split(":");
        client.execute((restHighLevelClient, requestOptions) -> restHighLevelClient.tasks().get(new GetTaskRequest(parts[0], Long.parseLong(parts[1])), requestOptions))
                .ifPresentOrElse(response -> {
                    if (response.isCompleted()) {
                        final long durationInSec = TimeUnit.SECONDS.convert(response.getTaskInfo().getRunningTimeNanos(), TimeUnit.NANOSECONDS);
                        final Duration duration = Duration.standardSeconds(durationInSec);
                        final TaskStatus taskStatus = getTaskStatus(response);
                        if (taskStatus.failures().isEmpty()) {
                            final String message = String.format(Locale.ROOT, "Index %s finished migration after %s. Total %d documents, updated %d, created %d, deleted %d.", index.getName(), humanReadable(duration), taskStatus.total(), taskStatus.updated(), taskStatus.created(), taskStatus.deleted());
                            logInfo(migration, message);
                            index.onFinished(duration, taskStatus);
                        } else {
                            final String failures = String.join(", ", taskStatus.failures());
                            final String message = String.format(Locale.ROOT, "Index %s migration failed after %s. Failures: %s.", index.getName(), humanReadable(duration), failures);
                            logError(migration, message, null);
                            index.onError(duration, taskStatus);
                        }
                    }
                }, () -> LOG.warn("Task for reindexing of {} not found!", index.getName()));
    }

    private String humanReadable(Duration duration) {
        return DurationFormatUtils.formatDurationWords(duration.getMillis(), true, true);
    }

    private TaskStatus getTaskStatus(GetTaskResponse response) {
        try {
            return objectMapper.readValue(response.getTaskInfo().getStatus().toString(), TaskStatus.class);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert TaskStatus", e);
            return TaskStatus.unknown();
        }
    }
}
