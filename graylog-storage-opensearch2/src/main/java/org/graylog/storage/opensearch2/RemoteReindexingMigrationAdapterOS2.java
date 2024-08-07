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
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.graylog.shaded.opensearch2.org.opensearch.OpenSearchException;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterGetSettingsRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.RequestOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.Response;
import org.graylog.shaded.opensearch2.org.opensearch.client.ResponseException;
import org.graylog.shaded.opensearch2.org.opensearch.client.tasks.TaskSubmissionResponse;
import org.graylog.shaded.opensearch2.org.opensearch.cluster.health.ClusterHealthStatus;
import org.graylog.shaded.opensearch2.org.opensearch.common.xcontent.json.JsonXContent;
import org.graylog.shaded.opensearch2.org.opensearch.core.common.bytes.BytesReference;
import org.graylog.shaded.opensearch2.org.opensearch.core.xcontent.ToXContent;
import org.graylog.shaded.opensearch2.org.opensearch.core.xcontent.XContentBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.reindex.ReindexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.index.reindex.RemoteInfo;
import org.graylog.shaded.opensearch2.org.opensearch.tasks.Task;
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
import org.graylog2.indexer.migration.RemoteIndex;
import org.graylog2.indexer.migration.RemoteReindexIndex;
import org.graylog2.indexer.migration.RemoteReindexMigration;
import org.graylog2.indexer.migration.TaskStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.resources.datanodes.DatanodeResolver;
import org.graylog2.rest.resources.datanodes.DatanodeRestApiProxy;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private final Indices indices;
    private final IndexSetRegistry indexSetRegistry;
    private final ClusterEventBus eventBus;
    private final ObjectMapper objectMapper;

    private final RemoteReindexMigrationService reindexMigrationService;

    private final DatanodeRestApiProxy datanodeRestApiProxy;

    @Inject
    public RemoteReindexingMigrationAdapterOS2(final OpenSearchClient client,
                                               final Indices indices,
                                               final IndexSetRegistry indexSetRegistry,
                                               final ClusterEventBus eventBus,
                                               final ObjectMapper objectMapper,
                                               RemoteReindexMigrationService reindexMigrationService,
                                               DatanodeRestApiProxy datanodeRestApiProxy) {
        this.client = client;
        this.indices = indices;
        this.indexSetRegistry = indexSetRegistry;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
        this.reindexMigrationService = reindexMigrationService;
        this.datanodeRestApiProxy = datanodeRestApiProxy;
    }

    @Override
    public String start(RemoteReindexRequest request) {
        final AggregatedConnectionResponse response = getAllIndicesFrom(request.uri(), request.username(), request.password(), request.trustUnknownCerts());
        final List<String> indices = isAllIndices(request.indices()) ? response.indices() : request.indices();
        final MigrationConfiguration migration = reindexMigrationService.saveMigration(MigrationConfiguration.forIndices(indices, response.certificates()));
        doStartMigration(migration, request);
        return migration.id();

    }

    private void doStartMigration(MigrationConfiguration migration, RemoteReindexRequest request) {
        try {
            new Thread(() -> {
                prepareCluster(request, migration);
                createIndicesInNewCluster(migration);
                startAsyncTasks(migration, request);
            }).start();
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

    private void prepareCluster(RemoteReindexRequest req, MigrationConfiguration migration) {
        final RemoteReindexAllowlist allowlist = new RemoteReindexAllowlist(req.uri(), req.allowlist());
        if (!allowlist.isClusterSettingMatching(clusterAllowlistSetting())) {
            // this is expected state for fresh datanode cluster - there is no value configured in the reindex.remote.allowlist
            // we have to add it to the configuration and wait till the whole cluster restarts
            logInfo(migration, "Preparing cluster for remote reindexing, setting allowlist to: " + req.allowlist());
            allowReindexing(allowlist, migration);
            waitForClusterRestart(allowlist, migration);
        } else {
            logInfo(migration, "Remote reindex allowlist already configured, skipping cluster configuration and restart.");
        }
    }

    private ReindexRequest createReindexRequest(final String index, final BytesReference query, URI uri, String username, String password, MigrationConfiguration migration) {
        final ReindexRequest reindexRequest = new ReindexRequest();
        reindexRequest
                .setRemoteInfo(new RemoteInfo(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), query, username, password, Map.of(), RemoteInfo.DEFAULT_SOCKET_TIMEOUT, RemoteInfo.DEFAULT_CONNECT_TIMEOUT))
                .setSourceIndices(index).setDestIndex(index).setShouldStoreResult(true);
        return reindexRequest;
    }

    @Override
    public RemoteReindexMigration status(@Nonnull String migrationID) {
        return reindexMigrationService.getMigration(migrationID)
                .map(migrationConfiguration -> {
                    final List<RemoteReindexIndex> indices = migrationConfiguration.indices()
                            .parallelStream()
                            .map(indexConfig -> indexConfig.taskId().flatMap(this::getTask).map(task -> taskToIndex(indexConfig.indexName(), task))
                                    .orElse(RemoteReindexIndex.noBackgroundTaskYet(indexConfig.indexName())))
                            .sorted(Comparator.comparing(RemoteReindexIndex::name))
                            .collect(Collectors.toList());
                    return new RemoteReindexMigration(migrationID, indices, migrationConfiguration.logs());
                }).orElse(RemoteReindexMigration.nonExistent(migrationID));
    }

    private RemoteReindexIndex taskToIndex(String indexName, GetTaskResponse task) {
        final DateTime created = new DateTime(task.task().startTimeInMillis(), DateTimeZone.UTC);
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
            return new RemoteReindexIndex(indexName, Status.RUNNING, created, duration, progress, null);
        }
    }

    private IndexMigrationProgress toProgress(TaskStatus status) {
        return new IndexMigrationProgress(status.total(), status.created(), status.updated(), status.deleted(), status.versionConflicts(), status.noops());
    }

    @Nullable
    private String getErrors(GetTaskResponse task) {
        if (task.error() != null) {
            return task.toString();
        } else if (task.task().status().hasFailures()) {
            return String.join(";", task.task().status().failures());
        } else if (task.response().failures() != null && !task.response().failures().isEmpty()) {
            return task.response().failures().stream()
                    .map(TaskResponseFailure::cause)
                    .filter(Objects::nonNull)
                    .map(f -> f.type() + ": " + f.reason())
                    .distinct()
                    .collect(Collectors.joining(";"));
        }
        return null;
    }

    @Override
    public IndexerConnectionCheckResult checkConnection(@Nonnull URI remoteHost, @Nullable String username, @Nullable String password, @Nullable String allowlist, boolean trustUnknownCerts) {
        try {
            final RemoteReindexAllowlist reindexAllowlist = new RemoteReindexAllowlist(remoteHost, allowlist);
            reindexAllowlist.validate();
            final AggregatedConnectionResponse results = getAllIndicesFrom(remoteHost, username, password, trustUnknownCerts);
            final List<RemoteIndex> indices = results.indices().stream()
                    .map(i -> new RemoteIndex(i, indexSetRegistry.isManagedIndex(i)))
                    .distinct()
                    .toList();
            if (results.error() != null && !results.error().isEmpty()) {
                return IndexerConnectionCheckResult.failure(results.error());
            } else {
                return IndexerConnectionCheckResult.success(indices);
            }
        } catch (Exception e) {
            return IndexerConnectionCheckResult.failure(e);
        }
    }

    private String clusterAllowlistSetting() {
        return client.execute((restHighLevelClient, requestOptions) -> {
            final ClusterGetSettingsRequest request = new ClusterGetSettingsRequest();
            request.includeDefaults(true);
            final ClusterGetSettingsResponse settings = restHighLevelClient.cluster().getSettings(request, requestOptions);
            return settings.getSetting("reindex.remote.allowlist");
        });
    }

    private void waitForClusterRestart(RemoteReindexAllowlist allowlist, MigrationConfiguration migration) {
        final var retryer = RetryerBuilder.<RemoteReindexConfigurationStatus>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(WAIT_BETWEEN_CONNECTION_ATTEMPTS, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(CONNECTION_ATTEMPTS))
                .withRetryListener(createClusterRestartWaitListener(migration))
                .retryIfException()
                .retryIfResult(status -> !status.isClusterReady())
                .build();
        try {
            retryer.call(() -> remoteReindexClusterState(allowlist));
            logInfo(migration, "Datanode cluster successfully reconfigured and restarted.");
        } catch (ExecutionException | RetryException e) {
            final String message = "Cluster failed to restart after " + CONNECTION_ATTEMPTS * WAIT_BETWEEN_CONNECTION_ATTEMPTS + " seconds.";
            logError(migration, message, e);
            throw new RuntimeException(message);
        }
    }

    @Nonnull
    private RetryListener createClusterRestartWaitListener(MigrationConfiguration migration) {
        return new RetryListener() {
            @Override
            public <V> void onRetry(Attempt<V> attempt) {
                if (attempt.hasResult()) {
                    final RemoteReindexConfigurationStatus status = (RemoteReindexConfigurationStatus) attempt.getResult();
                    final String message = String.format(Locale.ROOT, "Waiting for datanode cluster to reconfigure and restart, attempt %d. Cluster health: %s, allowlist configured: %b (cluster setting value: %s).", attempt.getAttemptNumber(), status.status(), status.allowlistConfigured(), status.clusterAllowlistSetting());
                    logInfo(migration, message);
                } else {
                    logInfo(migration, "Waiting for datanode cluster to reconfigure and restart, attempt #" + attempt.getAttemptNumber());
                }
            }
        };
    }

    private RemoteReindexConfigurationStatus remoteReindexClusterState(RemoteReindexAllowlist allowlist) {
        final ClusterHealthResponse clusterHealth = client.execute((restHighLevelClient, requestOptions) -> restHighLevelClient.cluster().health(new ClusterHealthRequest(), RequestOptions.DEFAULT));
        final String clusterAllowlistSetting = clusterAllowlistSetting();
        final boolean remoteReindexAllowed = allowlist.isClusterSettingMatching(clusterAllowlistSetting);
        return new RemoteReindexConfigurationStatus(remoteReindexAllowed, clusterAllowlistSetting, clusterHealth.getStatus());
    }

    private record RemoteReindexConfigurationStatus(boolean allowlistConfigured, String clusterAllowlistSetting,
                                                    ClusterHealthStatus status) {
        private boolean isClusterReady() {
            return allowlistConfigured && status.equals(ClusterHealthStatus.GREEN);
        }
    }

    void allowReindexing(RemoteReindexAllowlist allowlist, MigrationConfiguration migration) {
        if (migration.certificates() != null && !migration.certificates().isEmpty()) {
            eventBus.post(RemoteReindexAllowlistEvent.add(allowlist.value(), migration.certificates()));
        } else {
            eventBus.post(RemoteReindexAllowlistEvent.add(allowlist.value()));
        }
    }

    /**
     * Request indices list of a remote host, asking each datanode in the cluster. This also verifies that we can actually connect to the
     * remote host from each datanode. Additionally, the call delivers unknown SSL certificates that may be present. They
     * will be used to display better error message or transported back to the datanodes as trusted, if user decides so.
     */
    private AggregatedConnectionResponse getAllIndicesFrom(final URI uri, final String username, final String password, boolean trustUnknownCerts) {
        final ConnectionCheckRequest req = new ConnectionCheckRequest(uriToString(uri), username, password, trustUnknownCerts);
        final Map<String, ConnectionCheckResponse> responses = datanodeRestApiProxy.remoteInterface(DatanodeResolver.ALL_NODES_KEYWORD, DatanodeRemoteConnectionCheckResource.class, resource -> resource.opensearch(req));
        return new AggregatedConnectionResponse(responses);
    }

    private static String uriToString(URI uri){
        try {
            return uri.toURL().toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
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
        try {
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
        } catch (OpenSearchException e) {
            if (e.getCause() != null && e.getCause() instanceof ResponseException responseException) {
                if (responseException.getResponse().getStatusLine().getStatusCode() == 404) {
                    return Optional.empty();
                }
            }
            throw e;
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
        String message = String.format(Locale.ROOT, "Index %s finished migration after %s. Total %d documents, updated %d, created %d, deleted %d.", index, humanReadable(duration), taskStatus.total(), taskStatus.updated(), taskStatus.created(), taskStatus.deleted());
        if (taskStatus.noops() > 0 || taskStatus.versionConflicts() > 0) {
            message += String.format(Locale.ROOT, " %d documents were not migrated (%d version conflicts, %d ignored)", taskStatus.versionConflicts() + taskStatus.noops(), taskStatus.versionConflicts(), taskStatus.noops());
        }
        logInfo(migration, message);
    }

    private String humanReadable(Duration duration) {
        return DurationFormatUtils.formatDurationWords(duration.getMillis(), true, true);
    }
}
