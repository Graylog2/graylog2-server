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
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import models.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class ServerNodes {
    private static final Logger log = LoggerFactory.getLogger(ServerNodes.class);

    private final Random random = new Random();

    private AtomicReference<ImmutableList<Node>> nodes = new AtomicReference<>();

    private static ServerNodes INSTANCE;
    private static ImmutableList<Node> initialNodes;

    @Inject
    private ServerNodes(@Named("Initial Nodes") Node[] nodes) {
        log.info("Creating ServerNodes with initial nodes {}", (Object)nodes);
        initialNodes = ImmutableList.copyOf(nodes);
        setCurrentNodes(initialNodes);
        INSTANCE = this;
    }

    public static ServerNodes getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("ServerNodes.initialize() was not called.");
        }
        return INSTANCE;
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

    public void setCurrentNodes(List<Node> nodeList) {
        nodes.set(ImmutableList.copyOf(nodeList));
    }
}
