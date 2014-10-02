/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
