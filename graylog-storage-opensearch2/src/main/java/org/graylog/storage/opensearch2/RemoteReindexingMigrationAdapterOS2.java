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
import org.graylog.shaded.opensearch2.org.opensearch.client.RequestOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.Response;
import org.graylog.shaded.opensearch2.org.opensearch.client.tasks.TaskSubmissionResponse;
import org.graylog.shaded.opensearch2.org.opensearch.common.xcontent.json.JsonXContent;
import org.graylog.shaded.opensearch2.org.opensearch.core.common.bytes.BytesReference;
import org.graylog.shaded.opensearch2.org.opensearch.core.xcontent.ToXContent;
import org.graylog.shaded.opensearch2.org.opensearch.core.xcontent.XContentBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.reindex.ReindexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.index.reindex.RemoteInfo;
import org.graylog.shaded.opensearch2.org.opensearch.tasks.Task;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.datanode.RemoteReindexAllowlistEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.datanode.IndexMigrationConfiguration;
import org.graylog2.indexer.datanode.MigrationConfiguration;
import org.graylog2.indexer.datanode.RemoteReindexMigrationService;
import org.graylog2.indexer.datanode.RemoteReindexRequest;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.migration.IndexMigrationProgress;
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

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
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

    private final RemoteReindexMigrationService reindexMigrationService;

    @Inject
    public RemoteReindexingMigrationAdapterOS2(final OpenSearchClient client,
                                               final OkHttpClient httpClient,
                                               final NodeService<DataNodeDto> nodeService,
                                               final Indices indices,
                                               final IndexSetRegistry indexSetRegistry,
                                               final ClusterEventBus eventBus,
                                               final ObjectMapper objectMapper, RemoteReindexMigrationService reindexMigrationService) {
        this.client = client;
        this.httpClient = httpClient;
        this.nodeService = nodeService;
        this.indices = indices;
        this.indexSetRegistry = indexSetRegistry;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
        this.reindexMigrationService = reindexMigrationService;
    }

    @Override
    public String start(RemoteReindexRequest request) {
        final MigrationConfiguration migration = reindexMigrationService.saveMigration(MigrationConfiguration.forIndices(collectIndices(request)));
        doStartMigration(migration, request);
        return migration.id();
    }

    private List<String> collectIndices(RemoteReindexRequest request) {
        try {
            return isAllIndices(request.indices()) ? getAllIndicesFrom(request.uri(), request.username(), request.password()) : request.indices();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private void doStartMigration(MigrationConfiguration migration, RemoteReindexRequest request) {
        try {
            prepareCluster(request.allowlist());
            createIndicesInNewCluster(migration);
            startAsyncTasks(migration, request);
        } catch (Exception e) {
            LOG.error("Failed to start remote reindex migration", e);
            throw new RuntimeException(e);
        }
    }

    private void createIndicesInNewCluster(MigrationConfiguration migration) {
        migration.indices().forEach(index -> {
            if (!this.indices.exists(index.indexName())) {
                this.indices.create(index.indexName(), indexSetRegistry.getForIndex(index.indexName()).orElse(indexSetRegistry.getDefault()));
            } else {
                logInfo(migration, String.format(Locale.ROOT, "Index %s does already exist in target indexer. Data will be migrated into existing index.", index.indexName()));
            }
        });
    }

    private void prepareCluster(String allowlistAsString) {
        final var activeNodes = getAllActiveNodeIDs();
        List<String> allowlist = Arrays.asList(allowlistAsString.split(","));
        try {
            verifyRemoteReindexAllowlistSetting(allowlist);
        } catch (RemoteReindexNotAllowedException e) {
            // this is expected state for fresh datanode cluster - there is no value configured in the reindex.remote.allowlist
            // we have to add it to the configuration and wait till the whole cluster restarts
            allowReindexingFrom(allowlist);
            waitForClusterRestart(activeNodes);
        }

        // verify again, just to be sure that all the configuration is in place and vali
        verifyRemoteReindexAllowlistSetting(allowlist);
    }

    private ReindexRequest createReindexRequest(final String index, final BytesReference query, URI uri, String username, String password, MigrationConfiguration migration) {
        final ReindexRequest reindexRequest = new ReindexRequest();
        reindexRequest
                .setRemoteInfo(new RemoteInfo(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), query, username, password, Map.of(), RemoteInfo.DEFAULT_SOCKET_TIMEOUT, RemoteInfo.DEFAULT_CONNECT_TIMEOUT))
                .setSourceIndices(index).setDestIndex(index).setShouldStoreResult(true);
        return reindexRequest;
    }

    @Override
    public RemoteReindexMigration status(@NotNull String migrationID) {
        return reindexMigrationService.getMigration(migrationID)
                .map(migrationConfiguration -> {
                    final List<RemoteReindexIndex> indices = migrationConfiguration.indices()
                            .parallelStream()
                            .map(indexConfig -> indexConfig.taskId().flatMap(this::getTask).map(task -> taskToIndex(indexConfig.indexName(), task))
                                    .orElse(RemoteReindexIndex.notStartedYet(indexConfig.indexName())))
                            .sorted(Comparator.comparing(RemoteReindexIndex::name))
                            .collect(Collectors.toList());
                    return new RemoteReindexMigration(migrationID, indices, migrationConfiguration.logs());
                }).orElse(RemoteReindexMigration.nonExistent(migrationID));
    }

    private RemoteReindexIndex taskToIndex(String indexName, GetTaskResponse task) {
        final DateTime created = new DateTime(task.task().startTimeInMillis());
        Duration duration = getDuration(task);

        IndexMigrationProgress progress = toProgress(task.task().status());

        if (task.completed()) {
            final String errors = getErrors(task);
            if (errors != null) {
                return new RemoteReindexIndex(indexName, Status.ERROR, created, duration, progress, errors);
            } else {
                return new RemoteReindexIndex(indexName, Status.FINISHED, created, duration, progress, null);
            }
        } else {
            return new RemoteReindexIndex(indexName, Status.RUNNING, created, duration,progress, null);
        }
    }

    private IndexMigrationProgress toProgress(TaskStatus status) {
        return new IndexMigrationProgress(status.total(), status.created(), status.updated(), status.deleted());
    }

    @Nullable
    private String getErrors(GetTaskResponse task) {
        if (task.error() != null) {
            return task.error().type() + ": " + task.error().reason();
        } else if (task.task().status().hasFailures()) {
            return String.join(";", task.task().status().failures());
        }
        return null;
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

    public void verifyRemoteReindexAllowlistSetting(List<String> allowlistEntries) throws RemoteReindexNotAllowedException {
        final String allowlistSettingValue = client.execute((restHighLevelClient, requestOptions) -> {
            final ClusterGetSettingsRequest request = new ClusterGetSettingsRequest();
            request.includeDefaults(true);
            final ClusterGetSettingsResponse settings = restHighLevelClient.cluster().getSettings(request, requestOptions);
            return settings.getSetting("reindex.remote.allowlist");
        });

        // the value is not proper json, just something like [localhost:9201]. It should be safe to simply use String.contains,
        // but there is maybe a chance for mismatches and then we'd have to parse the value
        final boolean isRemoteReindexAllowed = !allowlistEntries.stream().allMatch(entry -> allowlistSettingValue.contains(entry));
        if (isRemoteReindexAllowed) {
            final String message = "Failed to configure reindex.remote.allowlist setting in the datanode cluster. Current setting value: " + allowlistSettingValue;
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

    void allowReindexingFrom(final List<String> allowlist) {
        eventBus.post(new RemoteReindexAllowlistEvent(allowlist, RemoteReindexAllowlistEvent.ACTION.ADD));
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

    private void startAsyncTasks(MigrationConfiguration migration, RemoteReindexRequest request) {
        final int threadsCount = Math.max(1, Math.min(request.threadsCount(), migration.indices().size()));

        final ExecutorService executorService = Executors.newFixedThreadPool(threadsCount, new ThreadFactoryBuilder()
                .setNameFormat("remote-reindex-migration-backend-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler(new Tools.LogUncaughtExceptionHandler(LOG))
                .build());

        migration.indices()
                .forEach(index -> executorService.submit(() -> executeReindexAsync(migration, request.uri(), request.username(), request.password(), index)));
    }

    private void executeReindexAsync(MigrationConfiguration migration, URI uri, String username, String password, IndexMigrationConfiguration index) {
        final String indexName = index.indexName();
        try (XContentBuilder builder = JsonXContent.contentBuilder().prettyPrint()) {
            final BytesReference query = BytesReference.bytes(matchAllQuery().toXContent(builder, ToXContent.EMPTY_PARAMS));
            logInfo(migration, "Executing async reindex for " + indexName);
            final TaskSubmissionResponse task = client.execute((c, requestOptions) -> {
                final RequestOptions withHeader = requestOptions.toBuilder()
                        .addHeader(Task.X_OPAQUE_ID, migration.id())
                        .build();
                return c.submitReindexTask(createReindexRequest(indexName, query, uri, username, password, migration), withHeader);
            });
            reindexMigrationService.assignTask(migration.id(), indexName, task.getTask());
            waitForTaskCompleted(migration, indexName, task.getTask());
        } catch (Exception e) {
            final String message = "Could not reindex index: " + indexName + " - " + e.getMessage();
            logError(migration, message, e);
        }
    }


    private void logInfo(MigrationConfiguration migration, String message) {
        LOG.info(message);
        reindexMigrationService.appendLogEntry(migration.id(), new LogEntry(DateTime.now(DateTimeZone.UTC), LogLevel.INFO, message));
    }

    private void logError(MigrationConfiguration migration, String message, Exception error) {
        if (error != null) {
            LOG.error(message, error);
        } else {
            LOG.error(message);
        }
        reindexMigrationService.appendLogEntry(migration.id(), new LogEntry(DateTime.now(DateTimeZone.UTC), LogLevel.ERROR, message));
    }

    private void waitForTaskCompleted(MigrationConfiguration migration, String indexName, String taskID) {
        while (taskIsStillRunning(taskID)) {
            sleep();
        }
        onTaskFinished(migration, indexName, taskID);
    }

    private boolean taskIsStillRunning(String taskID) {
        return getTask(taskID).map(t -> !t.completed()).orElse(true);
    }


    private static void sleep() {
        try {
            Thread.sleep(RemoteReindexingMigrationAdapterOS2.TASK_UPDATE_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<GetTaskResponse> getTask(String taskID) {
        final Response taskResponse = client.execute((restHighLevelClient, requestOptions) -> restHighLevelClient.getLowLevelClient().performRequest(
                new org.graylog.shaded.opensearch2.org.opensearch.client.Request("GET", "_tasks/" + taskID)
        ));

        if (taskResponse.getStatusLine().getStatusCode() == 404) {
            return Optional.empty();
        }

        try (InputStream is = taskResponse.getEntity().getContent()) {
            return Optional.of(objectMapper.readValue(is, GetTaskResponse.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTaskFinished(MigrationConfiguration migration, String index, String taskID) {
        final Optional<GetTaskResponse> task = getTask(taskID);
        task.ifPresent(t -> {
            final Duration duration = getDuration(t);
            final String errors = getErrors(t);
            if (errors != null) {
                onTaskFailure(migration, index, errors, duration);
            } else {
                onTaskSuccess(migration, index, t.task().status(), duration);
            }
        });
    }

    private static Duration getDuration(GetTaskResponse t) {
        final long durationInSec = TimeUnit.SECONDS.convert(t.task().runningTimeInNanos(), TimeUnit.NANOSECONDS);
        return Duration.standardSeconds(durationInSec);
    }

    private void onTaskFailure(MigrationConfiguration migration, String index, String error, Duration duration) {
        final String message = String.format(Locale.ROOT, "Index %s migration failed after %s: %s.", index, humanReadable(duration), error);
        logError(migration, message, null);
    }

    private void onTaskSuccess(MigrationConfiguration migration, String index, TaskStatus taskStatus, Duration duration) {
        final String message = String.format(Locale.ROOT, "Index %s finished migration after %s. Total %d documents, updated %d, created %d, deleted %d.", index, humanReadable(duration), taskStatus.total(), taskStatus.updated(), taskStatus.created(), taskStatus.deleted());
        logInfo(migration, message);
    }

    private String humanReadable(Duration duration) {
        return DurationFormatUtils.formatDurationWords(duration.getMillis(), true, true);
    }
}
