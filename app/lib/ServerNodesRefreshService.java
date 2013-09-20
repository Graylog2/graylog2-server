/**
 * Copyright 2013 Kay Roepke <kay@torch.sh>
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
package lib;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import models.Node;
import models.api.responses.NodeResponse;
import models.api.responses.NodeSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class ServerNodesRefreshService {
    private static final Logger log = LoggerFactory.getLogger(ServerNodesRefreshService.class);

    private final ApiClient api;
    private final ServerNodes serverNodes;
    private ScheduledExecutorService executor;

    @Inject
    private ServerNodesRefreshService(ApiClient api, ServerNodes serverNodes) {
        this.api = api;
        this.serverNodes = serverNodes;
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
            NodeResponse response = ApiClient.get(NodeResponse.class)
                    .path("/cluster/nodes")
                    .node(node)
                    .execute();
            int i = 0;
            for (NodeSummaryResponse nsr : response.nodes) {
                log.debug("Adding graylog2 server node {} to current set of nodes ({}/{})", nsr.transportAddress, ++i, response.nodes.size());
                newNodes.add(new Node(nsr));
            }
            return newNodes;
        }
    }

    public void start() {
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                refreshNodeList();
            }
        }, 0L, 5L, TimeUnit.SECONDS);
    }

    public void stop() {
        executor.shutdown();
    }

    public void refreshNodeList() {
        final Node initialNode = serverNodes.any();
        final RefreshOperation refreshOperation = new RefreshOperation(initialNode);
        final List<Node> nodeList;
        try {
            nodeList = refreshOperation.call();
        } catch (Exception e) {
            // TODO retry
            log.error("Could not retrieve graylog2 node list from node " + initialNode, e);
            throw new RuntimeException(e);
        }
        serverNodes.setCurrentNodes(nodeList);
    }
}
