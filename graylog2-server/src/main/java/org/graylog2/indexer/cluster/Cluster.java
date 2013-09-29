/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.indexer.cluster;

import com.google.common.collect.Lists;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.client.Client;
import org.graylog2.Core;

import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Cluster {

    private final Core server;
    private final Client c;

    public Cluster(Client client, Core server) {
        this.server = server;
        this.c = client;
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
        String[] indices = server.getDeflector().getAllDeflectorIndexNames();
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
             *
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

}
