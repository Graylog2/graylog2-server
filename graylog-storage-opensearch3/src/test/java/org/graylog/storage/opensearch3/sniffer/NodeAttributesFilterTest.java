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
package org.graylog.storage.opensearch3.sniffer;

import org.graylog.storage.opensearch3.sniffer.impl.NodeAttributesFilter;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NodeAttributesFilterTest {
    private final DiscoveredNode nodeOnRack23 = nodeOnRack(23);
    private final DiscoveredNode nodeOnRack42 = nodeOnRack(42);
    private final DiscoveredNode nodeWithNoExtraAttributes = new DiscoveredNode(
            "http", "no-attrs", 9200, Map.of("always", List.of("true")));

    @Test
    void doesNotFilterNodesIfNoFilterIsSet() {
        final List<DiscoveredNode> nodes = mockNodes();
        final var filter = new NodeAttributesFilter(false, null);
        assertThat(filter.filterNodes(nodes)).isEqualTo(nodes);
    }

    @Test
    void worksWithEmptyNodesListIfFilterIsSet() {
        final List<DiscoveredNode> nodes = Collections.emptyList();
        final var filter = new NodeAttributesFilter(true, "rack:42");
        assertThat(filter.filterNodes(nodes)).isEqualTo(nodes);
    }

    @Test
    void returnsNodesMatchingGivenFilter() {
        final List<DiscoveredNode> nodes = mockNodes();
        final var filter = new NodeAttributesFilter(true, "rack:42");
        assertThat(filter.filterNodes(nodes)).containsExactly(nodeOnRack42);
    }

    @Test
    void returnsNoNodesIfFilterDoesNotMatch() {
        final List<DiscoveredNode> nodes = mockNodes();
        final var filter = new NodeAttributesFilter(true, "location:alaska");
        assertThat(filter.filterNodes(nodes)).isEmpty();
    }

    @Test
    void returnsAllNodesIfFilterMatchesAll() {
        final List<DiscoveredNode> nodes = mockNodes();
        final var filter = new NodeAttributesFilter(true, "always:true");
        assertThat(filter.filterNodes(nodes)).isEqualTo(nodes);
    }

    @Test
    void returnsMatchingNodesIfGivenAttributeIsInList() {
        final var matchingNode = new DiscoveredNode("http", "multi-attr", 9200,
                Map.of("something", List.of("somevalue", "42", "pi")));
        final List<DiscoveredNode> nodes = List.of(matchingNode);
        final var filter = new NodeAttributesFilter(true, "something:42");
        assertThat(filter.filterNodes(nodes)).isEqualTo(nodes);
    }

    private DiscoveredNode nodeOnRack(int rackNo) {
        return new DiscoveredNode("http", "rack-" + rackNo, 9200,
                Map.of("rack", List.of(Integer.toString(rackNo)), "always", List.of("true")));
    }

    private List<DiscoveredNode> mockNodes() {
        return List.of(nodeOnRack42, nodeOnRack23, nodeWithNoExtraAttributes);
    }
}
