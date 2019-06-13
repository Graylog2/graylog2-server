/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
