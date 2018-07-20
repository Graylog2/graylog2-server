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
package org.graylog2.utilities;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GraphsTest {
    @Test
    public void emptyDirectedGraph() {
        final ImmutableGraph<String> emptyGraph = Graphs.emptyDirectedGraph();
        assertThat(emptyGraph.isDirected()).isTrue();
        assertThat(emptyGraph.nodes()).isEmpty();
        assertThat(emptyGraph.edges()).isEmpty();
    }

    @Test
    public void emptyUndirectedGraph() {
        final ImmutableGraph<String> emptyGraph = Graphs.emptyUndirectedGraph();
        assertThat(emptyGraph.isDirected()).isFalse();
        assertThat(emptyGraph.nodes()).isEmpty();
        assertThat(emptyGraph.edges()).isEmpty();
    }

    @Test
    public void emptyGraphWithTemplate() {
        final MutableGraph<String> templateGraph = GraphBuilder
                .directed()
                .allowsSelfLoops(true)
                .build();
        final ImmutableGraph<String> emptyGraph = Graphs.emptyGraph(templateGraph);
        assertThat(emptyGraph.isDirected()).isTrue();
        assertThat(emptyGraph.allowsSelfLoops()).isTrue();
        assertThat(emptyGraph.edges()).isEmpty();
        assertThat(emptyGraph.edges()).isEmpty();
    }

    @Test
    public void singletonDirectedGraph() {
        final ImmutableGraph<String> singletonGraph = Graphs.singletonDirectedGraph("Test");
        assertThat(singletonGraph.isDirected()).isTrue();
        assertThat(singletonGraph.nodes()).containsExactly("Test");
        assertThat(singletonGraph.edges()).isEmpty();
    }

    @Test
    public void singletonUndirectedGraph() {
        final ImmutableGraph<String> singletonGraph = Graphs.singletonUndirectedGraph("Test");
        assertThat(singletonGraph.isDirected()).isFalse();
        assertThat(singletonGraph.nodes()).containsExactly("Test");
        assertThat(singletonGraph.edges()).isEmpty();
    }

    @Test
    public void singletonGraphWithTemplate() {
        final MutableGraph<String> templateGraph = GraphBuilder
                .directed()
                .allowsSelfLoops(true)
                .build();
        final ImmutableGraph<String> singletonGraph = Graphs.singletonGraph(templateGraph, "Test");
        assertThat(singletonGraph.isDirected()).isTrue();
        assertThat(singletonGraph.allowsSelfLoops()).isTrue();
        assertThat(singletonGraph.nodes()).containsExactly("Test");
        assertThat(singletonGraph.edges()).isEmpty();
    }

    @Test
    public void mergeEmptyGraphs() {
        final ImmutableGraph<String> emptyGraph = Graphs.emptyDirectedGraph();
        final MutableGraph<String> emptyMutableGraph = GraphBuilder.from(emptyGraph).build();
        Graphs.merge(emptyMutableGraph, emptyGraph);
        assertThat(emptyMutableGraph.nodes()).isEmpty();
        assertThat(emptyMutableGraph.edges()).isEmpty();
    }

    @Test
    public void mergeIdenticalGraphs() {
        final ImmutableGraph<String> singletonGraph = Graphs.singletonDirectedGraph("Test");
        final MutableGraph<String> mutableGraph = GraphBuilder.from(singletonGraph).build();
        mutableGraph.addNode("Test");

        Graphs.merge(mutableGraph, singletonGraph);
        assertThat(mutableGraph).isEqualTo(singletonGraph);
        assertThat(mutableGraph).isEqualTo(mutableGraph);
    }

    @Test
    public void mergeOverlappingGraphs() {
        final ImmutableGraph<String> graph2 = Graphs.singletonDirectedGraph("Test1");
        final MutableGraph<String> graph1 = GraphBuilder.from(graph2).build();
        graph1.addNode("Test1");
        graph1.addNode("Test2");
        graph1.putEdge("Test1", "Test2");
        final MutableGraph<String> expectedGraph = GraphBuilder.from(graph2).build();
        expectedGraph.addNode("Test1");
        expectedGraph.addNode("Test2");
        expectedGraph.putEdge("Test1", "Test2");

        Graphs.merge(graph1, graph2);
        assertThat(graph1).isEqualTo(expectedGraph);
    }

    @Test
    public void mergeOverlappingGraphsWithInvertedEdges() {
        final MutableGraph<String> graph1 = GraphBuilder.directed().build();
        graph1.addNode("Test1");
        graph1.addNode("Test2");
        graph1.putEdge("Test1", "Test2");
        final MutableGraph<String> graph2 = GraphBuilder.directed().build();
        graph2.addNode("Test1");
        graph2.addNode("Test2");
        graph2.putEdge("Test2", "Test1");
        final MutableGraph<String> expectedGraph = GraphBuilder.from(graph2).build();
        expectedGraph.addNode("Test1");
        expectedGraph.addNode("Test2");
        expectedGraph.putEdge("Test1", "Test2");
        expectedGraph.putEdge("Test2", "Test1");

        Graphs.merge(graph1, graph2);
        assertThat(graph1).isEqualTo(expectedGraph);
    }

    @Test
    public void mergeDistinctGraphs() {
        final MutableGraph<String> graph1 = GraphBuilder.directed().build();
        graph1.addNode("Test1");
        graph1.addNode("Test2");
        graph1.putEdge("Test1", "Test2");

        final MutableGraph<String> graph2 = GraphBuilder.directed().build();
        graph2.addNode("Test3");
        graph2.addNode("Test4");
        graph2.putEdge("Test3", "Test4");

        final MutableGraph<String> expectedGraph = GraphBuilder.directed().build();
        expectedGraph.addNode("Test1");
        expectedGraph.addNode("Test2");
        expectedGraph.addNode("Test3");
        expectedGraph.addNode("Test4");
        expectedGraph.putEdge("Test1", "Test2");
        expectedGraph.putEdge("Test3", "Test4");

        Graphs.merge(graph1, graph2);
        assertThat(graph1).isEqualTo(expectedGraph);
    }
}