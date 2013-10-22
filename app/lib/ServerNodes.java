/*
 * Copyright 2013 TORCH UG
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
 */
package lib;

import com.google.common.collect.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lib.security.Graylog2ServerUnavailableException;
import models.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class ServerNodes {
    private static final Logger log = LoggerFactory.getLogger(ServerNodes.class);
    private final CopyOnWriteArrayList<Node> serverNodes = Lists.newCopyOnWriteArrayList();
    private final BiMap<Node, Node> configuredNodes = Maps.synchronizedBiMap(HashBiMap.<Node, Node>create());
    private final Random random = new Random();

    @Inject
    private ServerNodes(Node.Factory nodeFactory, @Named("Initial Nodes") URI[] nodeAddresses) {
        for (URI nodeAddress : nodeAddresses) {
            final Node configuredNode = nodeFactory.fromTransportAddress(nodeAddress);
            configuredNodes.put(configuredNode, configuredNode);
        }

        log.info("Creating ServerNodes with initial nodes {}", configuredNodes.keySet());
        // resolve the configured nodes:
        // we only know a transport address where we can reach them, but we don't know any node ids yet.
        // thus we do not know anything about them, and cannot even match them to node information coming
        // back from /system/cluster -> those all have node ids
        // ServerNodesRefreshService will do this for us, this class only deals with picking nodes from a list,
        // but does not update itself from external sources, this makes testing much easier
    }

    /**
     * Retrieves all currently active nodes.
     *
     * @return list of currently active nodes
     */
    public List<Node> all() {
        return all(false);
    }

    public List<Node> all(boolean allowInactive) {
        final Iterator<Node> nodeIterator;
        if (allowInactive) {
            nodeIterator = serverNodes.iterator();
        }
        else {
            nodeIterator = skipInactive(serverNodes);
        }
        final ImmutableList<Node> nodes = ImmutableList.copyOf(nodeIterator);
        if (nodes.isEmpty()) {
            throw new Graylog2ServerUnavailableException();
        }
        return nodes;
    }

    /**
     * Retrieve a random single active node.
     *
     * @return an active node
     */
    public Node any() {
        return any(false);
    }

    public Node any(boolean allowInactive) {
        final List<Node> all = all(allowInactive);
        final int i = random.nextInt(all.size());
        return all.get(i);
    }

    /**
     * Register nodes in the list of active nodes.
     *
     * The passed nodes are taken to be active, until this process knows it cannot reach them.
     *
     * @param nodes Nodes known to exist in the cluster
     */
    public void put(Collection<Node> nodes) {
        HashSet<Node> existingNodes = Sets.newHashSet(serverNodes);
        for (Node newNode : nodes) {
            for (Node serverNode : existingNodes) {
                log.debug("Checking new node {} against existing node {}", newNode, serverNode);
                if (newNode.equals(serverNode)) {
                    serverNode.merge(newNode);
                    existingNodes.remove(serverNode);
                    break;
                }
            }
        }

        serverNodes.addAllAbsent(nodes);
        logServerNodesState();
    }

    private void logServerNodesState() {
        if (log.isDebugEnabled()) {
            StringBuilder b = new StringBuilder();
            b.append("Node List").append('\n');
            for (Node serverNode : serverNodes) {
                b.append(' ');
                if (serverNode.isMaster()) {
                    b.append("* ");
                } else {
                    b.append("  ");
                }
                b.append(serverNode.getNodeId())
                        .append('\t')
                        .append(serverNode.getTransportAddress())
                        .append('\t')
                        .append(serverNode.isActive() ? "active" : "inactive");
                if (serverNode.getFailureCount() > 0) {
                    b.append('\t').append("failures: ").append(serverNode.getFailureCount());
                }
                final Node linkedNode = configuredNodes.inverse().get(serverNode);
                if (linkedNode != null) {
                    b.append('\t').append("via config node ").append(linkedNode.getTransportAddress());
                }
                b.append('\n');
            }
            log.debug(b.toString());
        }
    }

    public Map<String, Node> asMap() {
        Map<String, Node> map = Maps.newHashMap();
        for (Node serverNode : serverNodes) {
            map.put(serverNode.getNodeId(), serverNode);
        }

        return map;
    }

    private Iterator<Node> skipInactive(final Iterable<Node> iterable) {
        return new AbstractIterator<Node>() {
            Iterator<Node> in = iterable.iterator();
            @Override
            protected Node computeNext() {
                while (in.hasNext()) {
                    final Node next = in.next();
                    if (next.isActive()) {
                        return next;
                    }
                }
                return endOfData();
            }
        };
    }

    public Collection<Node> getConfiguredNodes() {
        return configuredNodes.keySet();
    }

    public void linkConfiguredNode(Node configuredNode, Node resolvedNode) {
        configuredNodes.put(configuredNode, resolvedNode);
    }

    public boolean isDisconnected() {
        return serverNodes.isEmpty();
    }
}
