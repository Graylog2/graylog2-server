/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.cluster;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.esplugin.ClusterStateMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


@Singleton
public class Cluster {
    private static final Logger LOG = LoggerFactory.getLogger(Cluster.class);

    private final Client c;
    private final IndexSetRegistry indexSetRegistry;
    private final ScheduledExecutorService scheduler;
    private final Duration requestTimeout;
    private final AtomicReference<Map<String, DiscoveryNode>> nodes = new AtomicReference<>();

    @Inject
    public Cluster(Client client,
                   IndexSetRegistry indexSetRegistry,
                   @Named("daemonScheduler") ScheduledExecutorService scheduler,
                   @Named("elasticsearch_request_timeout") Duration requestTimeout) {
        this.scheduler = scheduler;
        this.c = client;
        this.indexSetRegistry = indexSetRegistry;
        this.requestTimeout = requestTimeout;
        // unfortunately we can't use guice here, because elasticsearch and graylog2 use different injectors and we can't
        // get to the instance to bridge.
        ClusterStateMonitor.setCluster(this);
    }

    /**
     * Requests the cluster health for all indices managed by Graylog. (default: graylog_*)
     *
     * @return the cluster health response
     */
    public ClusterHealthResponse health() {
        ClusterHealthRequest request = new ClusterHealthRequest(indexSetRegistry.getIndexWildcards());
        return c.admin().cluster().health(request).actionGet();
    }

    /**
     * Requests the cluster health for the current write index. (deflector)
     *
     * This can be used to decide if the current write index is healthy and writable even when older indices have
     * problems.
     *
     * @return the cluster health response
     */
    public ClusterHealthResponse deflectorHealth() {
        ClusterHealthRequest request = new ClusterHealthRequest(indexSetRegistry.getWriteIndexAliases());
        return c.admin().cluster().health(request).actionGet();
    }

    public Map<String, NodeInfo> getDataNodes() {
        return getAllNodes().entrySet().stream()
                .filter(n -> n.getValue().getSettings().getAsBoolean("node.data", true))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, NodeInfo> getAllNodes() {
        final ClusterAdminClient clusterAdminClient = c.admin().cluster();
        final NodesInfoRequest request = clusterAdminClient.prepareNodesInfo()
                .all()
                .request();

        final ImmutableMap.Builder<String, NodeInfo> builder = ImmutableMap.builder();
        for (NodeInfo nodeInfo : clusterAdminClient.nodesInfo(request).actionGet().getNodes()) {
            builder.put(nodeInfo.getNode().id(), nodeInfo);
        }

        return builder.build();
    }

    public Map<String, NodeStats> getNodesStats(String... nodesIds) {
        final ClusterAdminClient clusterAdminClient = c.admin().cluster();
        final NodesStatsRequest request = clusterAdminClient.prepareNodesStats(nodesIds).request();
        final ImmutableMap.Builder<String, NodeStats> builder = ImmutableMap.builder();
        for (NodeStats nodeStats : clusterAdminClient.nodesStats(request).actionGet().getNodes()) {
            builder.put(nodeStats.getNode().id(), nodeStats);
        }

        return builder.build();
    }

    public String nodeIdToName(String nodeId) {
        final NodeInfo nodeInfo = getNodeInfo(nodeId);
        return nodeInfo == null ? "UNKNOWN" : nodeInfo.getNode().getName();

    }

    public String nodeIdToHostName(String nodeId) {
        final NodeInfo nodeInfo = getNodeInfo(nodeId);
        return nodeInfo == null ? "UNKNOWN" : nodeInfo.getHostname();
    }

    private NodeInfo getNodeInfo(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return null;
        }

        try {
            NodesInfoResponse r = c.admin().cluster().nodesInfo(new NodesInfoRequest(nodeId).all()).actionGet();
            return r.getNodesMap().get(nodeId);
        } catch (Exception e) {
            LOG.error("Could not read name of ES node.", e);
            return null;
        }
    }

    /**
     * Check if the Elasticsearch {@link org.elasticsearch.node.Node} is connected and that there are other nodes
     * in the cluster.
     *
     * @return {@code true} if the Elasticsearch client is up and the cluster contains other nodes, {@code false} otherwise
     */
    public boolean isConnected() {
        Map<String, DiscoveryNode> nodeMap = nodes.get();
        return nodeMap != null && !nodeMap.isEmpty();
    }

    /**
     * Check if the cluster health status is not {@link ClusterHealthStatus#RED} and that the
     * {@link IndexSetRegistry#isUp() deflector is up}.
     *
     * @return {@code true} if the the cluster is healthy and the deflector is up, {@code false} otherwise
     */
    public boolean isHealthy() {
        try {
            return health().getStatus() != ClusterHealthStatus.RED && indexSetRegistry.isUp();
        } catch (ElasticsearchException e) {
            LOG.trace("Couldn't determine Elasticsearch health properly", e);
            return false;
        }
    }

    /**
     * Check if the deflector (write index) health status is not {@link ClusterHealthStatus#RED} and that the
     * {@link IndexSetRegistry#isUp() deflector is up}.
     *
     * @return {@code true} if the deflector is healthy and up, {@code false} otherwise
     */
    public boolean isDeflectorHealthy() {
        try {
            return deflectorHealth().getStatus() != ClusterHealthStatus.RED && indexSetRegistry.isUp();
        } catch (ElasticsearchException e) {
            LOG.trace("Couldn't determine deflector index health properly", e);
            return false;
        }
    }

    /**
     * Blocks until the Elasticsearch cluster and current write index is healthy again or the given timeout fires.
     *
     * @param timeout the timeout value
     * @param unit the timeout unit
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

    public void updateDataNodeList(Map<String, DiscoveryNode> nodes) {
        LOG.debug("{} data nodes in cluster", nodes.size());
        this.nodes.set(nodes);
    }
}
