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
package org.graylog2.restclient.lib;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.api.responses.cluster.NodeSummaryResponse;
import org.graylog2.restclient.models.api.responses.cluster.NodesResponse;
import org.graylog2.restroutes.generated.routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.graylog2.restclient.lib.Configuration.apiTimeout;
import static org.graylog2.restclient.lib.Tools.rootCause;

@Singleton
public class ServerNodesRefreshService {
    private static final Logger log = LoggerFactory.getLogger(ServerNodesRefreshService.class);

    private final ApiClient api;
    private final ServerNodes serverNodes;
    private ScheduledExecutorService executor;
    private Node.Factory nodeFactory;

    @Inject
    private ServerNodesRefreshService(ApiClient api, ServerNodes serverNodes, Node.Factory nodeFactory) {
        this.api = api;
        this.serverNodes = serverNodes;
        this.nodeFactory = nodeFactory;
        executor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("servernodes-refresh-%d")
                        .setDaemon(true)
                        .build());
    }

    private class RefreshOperation implements Callable<List<Node>> {
        private final Node node;

        public RefreshOperation(Node node) {
            this.node = node;
        }
        @Override
        public List<Node> call() throws Exception {
            List<Node> newNodes = Lists.newArrayList();
            log.debug("Updating graylog2 server node list from node {}", node);
            NodesResponse response = api.path(routes.ClusterResource().nodes(), NodesResponse.class)
                    .node(node)
                    .unauthenticated()
                    .execute();
            int i = 0;
            for (NodeSummaryResponse nsr : response.nodes) {
                log.debug("Adding graylog2 server node " + nsr.transportAddress + " to current set of nodes ({}/{})", ++i, response.nodes.size());
                final Node newNode = nodeFactory.fromSummaryResponse(nsr);
                newNode.setActive(true);
                newNodes.add(newNode);
            }
            return newNodes;
        }
    }

    private void resolveConfiguredNodes() {
        // either we have just started and never seen any servers, or we lost connection to all servers in our cluster
        // resolve all configured nodes, to figure out the proper transport addresses in this network
        try {
            final Collection<Node> configuredNodes = serverNodes.getConfiguredNodes();
            final Map<Node, NodeSummaryResponse> responses =
                    api.path(routes.ClusterResource().node(), NodeSummaryResponse.class)
                            .nodes(configuredNodes)
                            .unauthenticated()
                            .timeout(apiTimeout("node_refresh", 2, TimeUnit.SECONDS))
                            .executeOnAll();
            List<Node> resolvedNodes = Lists.newArrayList();
            for (Map.Entry<Node, NodeSummaryResponse> nsr : responses.entrySet()) {
                if (nsr.getValue() == null) {
                    //skip empty responses, they indicate an error
                    continue;
                }
                final Node resolvedNode = nodeFactory.fromSummaryResponse(nsr.getValue());
                resolvedNode.setActive(true);
                resolvedNodes.add(resolvedNode);
                serverNodes.linkConfiguredNode(nsr.getKey(), resolvedNode);
            }
            serverNodes.put(resolvedNodes);
        } catch (Exception e) {
            log.error("Resolving configured nodes failed", e);
        }
    }

    public void start() {
        // try to resolve the configured nodes immediately on startup, so we can rely on this being done in tests.
        // won't make a difference to a production instance (except delay startup sequence slightly)
        resolveConfiguredNodes();

        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    refreshNodeList();
                } catch (Graylog2ServerUnavailableException e) {
                    resolveConfiguredNodes();
                }

            }
        }, 0L, 5L, TimeUnit.SECONDS);
    }

    public void stop() {
        executor.shutdown();
    }

    /**
     * Re-reads the cluster node state from a server.
     *
     * @return true if a cluster node list could be read, false if the read failed
     */
    private boolean refreshNodeList() {
        final Node initialNode = serverNodes.any();
        final RefreshOperation refreshOperation = new RefreshOperation(initialNode);
        final List<Node> nodeList;
        try {
            nodeList = refreshOperation.call();
            serverNodes.put(nodeList);
            return true;
        } catch (Exception e) {
            log.warn("Could not retrieve graylog2 node list from node " + initialNode + ". Retrying automatically.",
                    rootCause(e));
        }
        return false;
    }
}
