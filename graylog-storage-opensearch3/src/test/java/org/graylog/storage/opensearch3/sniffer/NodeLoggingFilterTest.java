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

import org.graylog.storage.opensearch3.sniffer.impl.NodeLoggingFilter;
import org.graylog2.configuration.ElasticsearchClientConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NodeLoggingFilterTest {

    @Test
    void returnsNodesUnmodified() {
        final var config = mock(ElasticsearchClientConfiguration.class);
        when(config.isNodeActivityLogger()).thenReturn(true);
        final var filter = new NodeLoggingFilter(config);

        final List<DiscoveredNode> nodes = List.of(
                new DiscoveredNode("http", "host1", 9200, Collections.emptyMap()),
                new DiscoveredNode("http", "host2", 9200, Collections.emptyMap())
        );

        assertThat(filter.filterNodes(nodes)).isEqualTo(nodes);
    }

    @Test
    void enabledReflectsConfiguration() {
        final var config = mock(ElasticsearchClientConfiguration.class);
        when(config.isNodeActivityLogger()).thenReturn(false);
        final var filter = new NodeLoggingFilter(config);
        assertThat(filter.enabled()).isFalse();
    }
}
