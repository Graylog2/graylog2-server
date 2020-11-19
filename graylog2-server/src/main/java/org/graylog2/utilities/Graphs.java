/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.utilities;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;

public final class Graphs {
    private static final ImmutableGraph EMPTY_DIRECTED_GRAPH = ImmutableGraph.copyOf(GraphBuilder.directed().build());
    private static final ImmutableGraph EMPTY_UNDIRECTED_GRAPH = ImmutableGraph.copyOf(GraphBuilder.undirected().build());

    private Graphs() {
    }

    /**
     * Returns an empty directed graph (immutable).
     *
     * @param <N> The class of the nodes
     * @return an empty directed graph
     */
    @SuppressWarnings("unchecked")
    public static <N> ImmutableGraph<N> emptyDirectedGraph() {
        return EMPTY_DIRECTED_GRAPH;
    }

    /**
     * Returns an empty undirected graph (immutable).
     *
     * @param <N> The class of the nodes
     * @return an empty undirected graph
     */
    @SuppressWarnings("unchecked")
    public static <N> ImmutableGraph<N> emptyUndirectedGraph() {
        return EMPTY_UNDIRECTED_GRAPH;
    }

    /**
     * Returns an empty graph (immutable) initialized with all properties queryable from {@code graph}.
     *
     * @param graph The graph to use as template for the created graph
     * @param <N>   The class of the nodes
     * @return an empty graph
     * @see GraphBuilder#from(Graph)
     */
    public static <N> ImmutableGraph<N> emptyGraph(Graph<N> graph) {
        return ImmutableGraph.copyOf(GraphBuilder.from(graph).build());
    }

    /**
     * Returns an immutable directed graph, containing only the specified node.
     *
     * @param node The single node in the returned graph
     * @param <N>  The class of the nodes
     * @return an immutable directed graph with a single node
     */
    public static <N> ImmutableGraph<N> singletonDirectedGraph(N node) {
        final MutableGraph<N> graph = GraphBuilder.directed().build();
        graph.addNode(node);
        return ImmutableGraph.copyOf(graph);
    }

    /**
     * Returns an immutable undirected graph, containing only the specified node.
     *
     * @param node The single node in the returned graph
     * @param <N>  The class of the nodes
     * @return an immutable undirected graph with a single node
     */
    public static <N> ImmutableGraph<N> singletonUndirectedGraph(N node) {
        final MutableGraph<N> graph = GraphBuilder.undirected().build();
        graph.addNode(node);
        return ImmutableGraph.copyOf(graph);
    }

    /**
     * Returns an immutable graph, containing only the specified node.
     *
     * @param graph The graph to use as template for the created graph
     * @param node  The single node in the returned graph
     * @param <N>   The class of the nodes
     * @return an immutable graph with a single node
     * @see GraphBuilder#from(Graph)
     */
    public static <N> ImmutableGraph<N> singletonGraph(Graph<N> graph, N node) {
        final MutableGraph<N> mutableGraph = GraphBuilder.from(graph).build();
        mutableGraph.addNode(node);
        return ImmutableGraph.copyOf(mutableGraph);
    }

    /**
     * Merge all nodes and edges of two graphs.
     *
     * @param graph1 A {@link MutableGraph} into which all nodes and edges of {@literal graph2} will be merged
     * @param graph2 The {@link Graph} whose nodes and edges will be merged into {@literal graph1}
     * @param <N>    The class of the nodes
     */
    public static <N> void merge(MutableGraph<N> graph1, Graph<N> graph2) {
        for (N node : graph2.nodes()) {
            graph1.addNode(node);
        }
        for (EndpointPair<N> edge : graph2.edges()) {
            graph1.putEdge(edge.nodeU(), edge.nodeV());
        }
    }
}
