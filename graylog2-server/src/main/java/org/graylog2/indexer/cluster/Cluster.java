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
package org.graylog2.indexer.cluster;

import com.github.joschi.jadconfig.util.Duration;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettings;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.indexer.cluster.health.NodeFileDescriptorStats;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.rest.models.system.indexer.responses.ClusterHealth;
import org.graylog2.system.stats.elasticsearch.ElasticsearchStats;
import org.graylog2.system.stats.elasticsearch.ShardStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Singleton
public class Cluster {
    private static final Logger LOG = LoggerFactory.getLogger(Cluster.class);

    private final IndexSetRegistry indexSetRegistry;
    private final ScheduledExecutorService scheduler;
    private final Duration requestTimeout;
    private final ClusterAdapter clusterAdapter;

    @Inject
    public Cluster(IndexSetRegistry indexSetRegistry,
                   @Named("daemonScheduler") ScheduledExecutorService scheduler,
                   @Named("elasticsearch_socket_timeout") Duration requestTimeout,
                   ClusterAdapter clusterAdapter) {
        this.scheduler = scheduler;
        this.indexSetRegistry = indexSetRegistry;
        this.requestTimeout = requestTimeout;
        this.clusterAdapter = clusterAdapter;
    }

    /**
     * Requests the cluster health for all indices managed by Graylog. (default: graylog_*)
     *
     * @return the cluster health response
     */
    public Optional<HealthStatus> health() {
        return clusterAdapter.health(allIndexWildcards());
    }

    private List<String> allIndexWildcards() {
        return Arrays.asList(indexSetRegistry.getIndexWildcards());
    }

    /**
     * Requests the cluster health for the current write index. (deflector)
     *
     * This can be used to decide if the current write index is healthy and writable even when older indices have
     * problems.
     *
     * @return the cluster health response
     */
    public Optional<HealthStatus> deflectorHealth() {
        return clusterAdapter.health(Arrays.asList(indexSetRegistry.getWriteIndexAliases()));
    }

    public Set<NodeFileDescriptorStats> getFileDescriptorStats() {
        return clusterAdapter.fileDescriptorStats();
    }

    public Set<NodeDiskUsageStats> getDiskUsageStats() {
        return clusterAdapter.diskUsageStats();
    }

    public ClusterAllocationDiskSettings getClusterAllocationDiskSettings() {
        return clusterAdapter.clusterAllocationDiskSettings();
    }

    public Optional<String> nodeIdToName(String nodeId) {
        return clusterAdapter.nodeIdToName(nodeId);
    }

    public Optional<String> nodeIdToHostName(String nodeId) {
        return clusterAdapter.nodeIdToHostName(nodeId);
    }

    /**
     * Check if Elasticsearch is available and that there are data nodes in the cluster.
     *
     * @return {@code true} if the Elasticsearch client is up and the cluster contains data nodes, {@code false} otherwise
     */
    public boolean isConnected() {
        return clusterAdapter.isConnected();
    }

    /**
     * Check if the cluster health status is not {@literal RED} and that the
     * {@link IndexSetRegistry#isUp() deflector is up}.
     *
     * @return {@code true} if the the cluster is healthy and the deflector is up, {@code false} otherwise
     */
    public boolean isHealthy() {
        return health()
                .map(health -> !health.equals(HealthStatus.Red) && indexSetRegistry.isUp())
                .orElse(false);
    }

    /**
     * Check if the deflector (write index) health status is not {@literal RED} and that the
     * {@link IndexSetRegistry#isUp() deflector is up}.
     *
     * @return {@code true} if the deflector is healthy and up, {@code false} otherwise
     */
    public boolean isDeflectorHealthy() {
        return deflectorHealth()
                .map(health -> !health.equals(HealthStatus.Red) && indexSetRegistry.isUp())
                .orElse(false);
    }

    /**
     * Blocks until the Elasticsearch cluster and current write index is healthy again or the given timeout fires.
     *
     * @param timeout the timeout value
     * @param unit    the timeout unit
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public void waitForConnectedAndDeflectorHealthy(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        LOG.debug("Waiting until the write-active index is healthy again, checking once per second.");

        final CountDownLatch latch = new CountDownLatch(1);
        final ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (isConnected() && isDeflectorHealthy()) {
                    LOG.debug("Write-active index is healthy again, unblocking waiting threads.");
                    latch.countDown();
                }
            } catch (Exception ignore) {
            } // to not cancel the schedule
        }, 0, 1, TimeUnit.SECONDS); // TODO should this be configurable?

        final boolean waitSuccess = latch.await(timeout, unit);
        scheduledFuture.cancel(true); // Make sure to cancel the task to avoid task leaks!

        if (!waitSuccess) {
            throw new TimeoutException("Write-active index didn't get healthy within timeout");
        }
    }

    /**
     * Blocks until the Elasticsearch cluster and current write index is healthy again or the default timeout fires.
     *
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public void waitForConnectedAndDeflectorHealthy() throws InterruptedException, TimeoutException {
        waitForConnectedAndDeflectorHealthy(requestTimeout.getQuantity(), requestTimeout.getUnit());
    }

    public Optional<String> clusterName() {
        return clusterAdapter.clusterName(allIndexWildcards());
    }

    public Optional<ClusterHealth> clusterHealthStats() {
        return clusterAdapter.clusterHealthStats(allIndexWildcards());
    }

    public ElasticsearchStats elasticsearchStats() {
        final List<String> indices = Arrays.asList(indexSetRegistry.getIndexWildcards());
        final org.graylog2.system.stats.elasticsearch.ClusterStats clusterStats = clusterAdapter.clusterStats();

        final PendingTasksStats pendingTasksStats = clusterAdapter.pendingTasks();

        final ShardStats shardStats = clusterAdapter.shardStats(indices);
        final org.graylog2.system.stats.elasticsearch.ClusterHealth clusterHealth = org.graylog2.system.stats.elasticsearch.ClusterHealth.from(
                shardStats,
                pendingTasksStats
        );
        final HealthStatus healthStatus = clusterAdapter.health(indices).orElseThrow(() -> new IllegalStateException("Unable to retrieve cluster health."));

        return ElasticsearchStats.create(
                clusterStats.clusterName(),
                clusterStats.clusterVersion(),
                healthStatus,
                clusterHealth,
                clusterStats.nodesStats(),
                clusterStats.indicesStats()
        );
    }
}
