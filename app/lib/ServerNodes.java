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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import models.Node;
import models.api.responses.NodeResponse;
import models.api.responses.NodeSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ServerNodes {
    private static final Logger log = LoggerFactory.getLogger(ServerNodes.class);

    private final Random random;

    private AtomicReference<ImmutableList<Node>> nodes = new AtomicReference<>();

    private ScheduledExecutorService executor;

    private static ServerNodes INSTANCE;
    private static ImmutableList<Node> initialNodes;

    private ServerNodes() {
        random = new Random();

        final Node initialNode = initialNodes.get(random.nextInt(initialNodes.size()));
        final NodeRefreshOperation refreshOperation = new NodeRefreshOperation(initialNode);
        final List<Node> nodeList;
        try {
            nodeList = refreshOperation.call();
        } catch (Exception e) {
            // TODO retry
            log.error("Could not retrieve graylog2 node list from initial node {}", initialNode);
            throw new RuntimeException(e);
        }
        nodes.set(ImmutableList.copyOf(nodeList));

        executor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("node-refresh-").setDaemon(true).build()
        );
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                refreshNodeList();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    public static void initialize(Node... nodes) {
        initialNodes = ImmutableList.copyOf(nodes);
        INSTANCE = new ServerNodes();
    }

    public static ServerNodes getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("ServerNodes.initialize() was not called.");
        }
        return INSTANCE;
    }

    public void refreshNodeList() {
        final Node initialNode = any();
        final NodeRefreshOperation refreshOperation = new NodeRefreshOperation(initialNode);
        final List<Node> nodeList;
        try {
            nodeList = refreshOperation.call();
        } catch (Exception e) {
            // TODO retry
            log.error("Could not retrieve graylog2 node list from node {}", initialNode);
            throw new RuntimeException(e);
        }
        nodes.set(ImmutableList.copyOf(nodeList));
    }

    public static List<Node> all() {
        return getInstance().nodes.get();
    }

    public static Node any() {
        final ServerNodes instance = getInstance();
        final ImmutableList<Node> nodeList = instance.nodes.get();
        return nodeList.get(instance.random.nextInt(nodeList.size()));
    }

    public static Map<String, Node> asMap() {
        Map<String, Node> map = Maps.newHashMap();
        for (Node node : all()) {
            map.put(node.getNodeId(), node);
        }

        return map;
    }

    private class NodeRefreshOperation implements Callable<List<Node>> {
        private final Node node;

        public NodeRefreshOperation(Node node) {
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

}
