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
package org.graylog.plugins.views.search.util;

import com.google.common.graph.Graph;
import info.leadinglight.jdot.Node;
import info.leadinglight.jdot.enums.GraphType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class GraphToDot {
    private static final Logger LOG = LoggerFactory.getLogger(GraphToDot.class);

    private GraphToDot() {
    }

    public static <N> String toDot(Graph<N> graph, Function<N, String> idFunction, Function<N, String> labelFunction) {
        final info.leadinglight.jdot.Graph g = new info.leadinglight.jdot.Graph("QueryPlan");
        g.setType(graph.isDirected() ? GraphType.digraph : GraphType.graph);

        graph.nodes().forEach(node -> {
            final Node n = new Node(idFunction.apply(node));
            n.setLabel(labelFunction.apply(node));
            g.addNode(n);
        });
        graph.edges().forEach(edge -> {
            g.addEdge(idFunction.apply(edge.source()), idFunction.apply(edge.target()));
        });
        return g.toDot();
    }
}
