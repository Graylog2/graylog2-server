package org.graylog.storage.elasticsearch7;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Node;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.sniff.NodesSniffer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FilteredElasticsearchNodesSnifferTest {
    private final Node nodeOnRack23 = nodeOnRack(23);
    private final Node nodeOnRack42 = nodeOnRack(42);
    private final Node nodeWithNoAttributes = mockNode(Collections.emptyMap());

    @Test
    void doesNotFilterNodesIfNoFilterIsSet() throws Exception {
        final List<Node> nodes = mockNodes();

        final NodesSniffer nodesSniffer = new FilteredElasticsearchNodesSniffer(mockSniffer(nodes), null, null);

        assertThat(nodesSniffer.sniff()).isEqualTo(nodes);
    }

    @Test
    void worksWithEmptyNodesListIfFilterIsSet() throws Exception {
        final List<Node> nodes = Collections.emptyList();

        final NodesSniffer nodesSniffer = new FilteredElasticsearchNodesSniffer(mockSniffer(nodes), "rack", "42");

        assertThat(nodesSniffer.sniff()).isEqualTo(nodes);
    }

    @Test
    void returnsNodesMatchingGivenFilter() throws Exception {
        final List<Node> nodes = mockNodes();

        final NodesSniffer nodesSniffer = new FilteredElasticsearchNodesSniffer(mockSniffer(nodes), "rack", "42");

        assertThat(nodesSniffer.sniff()).containsExactly(nodeOnRack42);
    }

    @Test
    void returnsNoNodesIfFilterDoesNotMatch() throws Exception {
        final List<Node> nodes = mockNodes();

        final NodesSniffer nodesSniffer = new FilteredElasticsearchNodesSniffer(mockSniffer(nodes), "location", "alaska");

        assertThat(nodesSniffer.sniff()).isEmpty();
    }

    @Test
    void returnsAllNodesIfFilterMatchesAll() throws Exception {
        final List<Node> nodes = mockNodes();

        final NodesSniffer nodesSniffer = new FilteredElasticsearchNodesSniffer(mockSniffer(nodes), "always", "true");

        assertThat(nodesSniffer.sniff()).isEqualTo(nodes);
    }

    @Test
    void returnsMatchingNodesIfGivenAttributeIsInList() throws Exception {
        final Node matchingNode = mockNode(ImmutableMap.of(
                "something", ImmutableList.of("somevalue", "42", "pi")
        ));
        final List<Node> nodes = Collections.singletonList(matchingNode);

        final NodesSniffer nodesSniffer = new FilteredElasticsearchNodesSniffer(mockSniffer(nodes), "something", "42");

        assertThat(nodesSniffer.sniff()).isEqualTo(nodes);
    }

    private Node nodeOnRack(int rackNo) {
        return mockNode(ImmutableMap.of(
                "rack", ImmutableList.of(Integer.toString(rackNo))
        ));
    }

    private Node mockNode(Map<String, List<String>> attributes) {
        final Node node = mock(Node.class);
        final Map<String, List<String>> nodeAttributes = new ImmutableMap.Builder<String, List<String>>()
                .put("always", Collections.singletonList("true"))
                .putAll(attributes)
                .build();
        when(node.getAttributes()).thenReturn(nodeAttributes);
        return node;
    }

    private List<Node> mockNodes() {
        return ImmutableList.of(nodeOnRack42, nodeOnRack23, nodeWithNoAttributes);
    }

    private NodesSniffer mockSniffer(List<Node> nodes) throws IOException {
        final NodesSniffer mockSniffer = mock(NodesSniffer.class);
        when(mockSniffer.sniff()).thenReturn(nodes);
        return mockSniffer;
    }
}
