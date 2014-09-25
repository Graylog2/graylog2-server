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
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.graylog2.indexer.Deflector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Singleton
public class Cluster {
    private static final Logger LOG = LoggerFactory.getLogger(Cluster.class);

    private final Client c;
    private final Deflector deflector;

    @Inject
    public Cluster(Node node, Deflector deflector) {
        this.c = node.client();
        this.deflector = deflector;
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
     * is not {@link ClusterHealthStatus#RED}.
     *
     * @return {@code true} if the Elasticsearch client is up and the cluster is healthy, {@code false} otherwise
     */
    public boolean isConnectedAndHealthy() {
        try {
            return getHealth() != ClusterHealthStatus.RED;
        } catch (ElasticsearchException e) {
            LOG.trace("Couldn't determine Elasticsearch health properly", e);
            return false;
        }
    }
}
