package org.graylog.plugins.enterprise.search.util;

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
