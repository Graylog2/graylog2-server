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

import org.graylog.storage.opensearch3.sniffer.impl.DatanodesSniffer;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatanodesSnifferTest {

    @SuppressWarnings("unchecked")
    private final NodeService<DataNodeDto> nodeService = mock(NodeService.class);

    @Test
    void sniffReturnsAvailableDataNodes() throws IOException {
        final DataNodeDto node1 = DataNodeDto.builder()
                .setId("id1")
                .setTransportAddress("https://datanode1:9200")
                .setDataNodeStatus(DataNodeStatus.AVAILABLE)
                .build();

        final DataNodeDto node2 = DataNodeDto.builder()
                .setId("id2")
                .setTransportAddress("https://datanode2:9200")
                .setDataNodeStatus(DataNodeStatus.AVAILABLE)
                .build();

        final DataNodeDto unavailable = DataNodeDto.builder()
                .setId("id3")
                .setTransportAddress("https://down-node:9200")
                .setDataNodeStatus(DataNodeStatus.UNAVAILABLE)
                .build();

        when(nodeService.allActive()).thenReturn(Map.of(
                "id1", node1,
                "id2", node2,
                "id3", unavailable
        ));

        final var sniffer = new DatanodesSniffer(nodeService, true);
        final var nodes = sniffer.sniff();

        assertThat(nodes).hasSize(2);
        assertThat(nodes).anySatisfy(n -> {
            assertThat(n.host()).isEqualTo("datanode1");
            assertThat(n.port()).isEqualTo(9200);
            assertThat(n.scheme()).isEqualTo("https");
        });
        assertThat(nodes).anySatisfy(n -> {
            assertThat(n.host()).isEqualTo("datanode2");
            assertThat(n.port()).isEqualTo(9200);
            assertThat(n.scheme()).isEqualTo("https");
        });
    }

    @Test
    void enabledOnlyWhenRunsWithDataNode() {
        assertThat(new DatanodesSniffer(nodeService, true).enabled()).isTrue();
        assertThat(new DatanodesSniffer(nodeService, false).enabled()).isFalse();
    }

    @Test
    void sniffReturnsEmptyWhenNoActiveNodes() throws IOException {
        when(nodeService.allActive()).thenReturn(Collections.emptyMap());
        final var sniffer = new DatanodesSniffer(nodeService, true);
        assertThat(sniffer.sniff()).isEmpty();
    }
}
