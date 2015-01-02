/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.cluster;

import com.google.common.collect.Lists;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.node.Node;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.esplugin.ClusterStateMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class Cluster {
    private static final Logger LOG = LoggerFactory.getLogger(Cluster.class);

    private final Client c;
    private final Deflector deflector;
    private final AtomicReference<Map<String, DiscoveryNode>> nodes = new AtomicReference<>();
    private ScheduledExecutorService scheduler;

    @Inject
    public Cluster(Node node, Deflector deflector, @Named("daemonScheduler") ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
        this.c = node.client();
        this.deflector = deflector;
        // unfortunately we can't use guice here, because elasticsearch and graylog2 use different injectors and we can't
        // get to the instance to bridge.
        ClusterStateMonitor.setCluster(this);
    }

    public String getName() {
        return health().getClusterName();
    }

    public ClusterHealthStatus getHealth() {
        return health().getStatus();
    }

    public int getActiveShards() {
        return health().getActiveShards();
    }

    public int getInitializingShards() {
        return health().getInitializingShards();
    }

    public int getUnassignedShards() {
        return health().getUnassignedShards();
    }

    public int getRelocatingShards() {
        return health().getRelocatingShards();
    }

    private ClusterHealthResponse health() {
        String[] indices = deflector.getAllDeflectorIndexNames();
        return c.admin().cluster().health(new ClusterHealthRequest(indices)).actionGet();
    }

    public int getNumberOfNodes() {
        return c.admin().cluster().nodesInfo(new NodesInfoRequest().all()).actionGet().getNodes().length;
    }

    public List<NodeInfo> getDataNodes() {
        List<NodeInfo> dataNodes = Lists.newArrayList();

        for (NodeInfo nodeInfo : getAllNodes()) {
            /*
             * We are setting node.data to false for our graylog2-server nodes.
             * If it's not set or not false it is a data storing node.
             */
            String isData = nodeInfo.getSettings().get("node.data");
            if (isData != null && isData.equals("false")) {
                continue;
            }

            dataNodes.add(nodeInfo);
        }

        return dataNodes;
    }

    public List<NodeInfo> getAllNodes() {
        return Lists.newArrayList(c.admin().cluster().nodesInfo(new NodesInfoRequest().all()).actionGet().getNodes());
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
     * Check if the Elasticsearch {@link Node} is connected and that the cluster health status
     * is not {@link ClusterHealthStatus#RED} and that the {@link org.graylog2.indexer.Deflector#isUp() deflector is up}.
     *
     * @return {@code true} if the Elasticsearch client is up and the cluster is healthy and the deflector is up, {@code false} otherwise
     */
    public boolean isConnectedAndHealthy() {
        Map<String, DiscoveryNode> nodeMap = nodes.get();
        if (nodeMap == null || nodeMap.isEmpty()) {
            return false;
        }
        if (!deflector.isUp()) {
            return false;
        }
        try {
            return getHealth() != ClusterHealthStatus.RED;
        } catch (ElasticsearchException e) {
            LOG.trace("Couldn't determine Elasticsearch health properly", e);
            return false;
        }
    }

    public void waitForConnectedAndHealthy() throws InterruptedException {
        LOG.debug("Waiting until cluster connection comes back and cluster is healthy, checking once per second.");

        final CountDownLatch latch = new CountDownLatch(1);
        final ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isConnectedAndHealthy()) {
                        LOG.debug("Cluster is healthy again, unblocking waiting threads.");
                        latch.countDown();
                    }
                } catch (Exception ignore) {} // to not cancel the schedule
            }
        }, 0, 1, TimeUnit.SECONDS); // TODO should this be configurable?

        latch.await();
        scheduledFuture.cancel(true); // Make sure to cancel the task to avoid task leaks!
    }

    public void updateDataNodeList(Map<String, DiscoveryNode> nodes) {
        LOG.debug("{} data nodes in cluster", nodes.size());
        this.nodes.set(nodes);
    }
}
