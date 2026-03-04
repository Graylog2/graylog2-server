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

import org.graylog.storage.opensearch3.DynamicTransport;
import org.graylog.storage.opensearch3.OfficialOpensearchClientProvider;
import org.graylog2.configuration.ElasticsearchClientConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.transport.OpenSearchTransport;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NodeDiscoveryPeriodicalTest {

    private SnifferAggregator snifferAggregator;
    private OfficialOpensearchClientProvider clientProvider;
    private DynamicTransport dynamicTransport;
    private ElasticsearchClientConfiguration configuration;
    private NodeDiscoveryPeriodical periodical;

    @BeforeEach
    void setUp() {
        snifferAggregator = mock(SnifferAggregator.class);
        clientProvider = mock(OfficialOpensearchClientProvider.class);
        dynamicTransport = mock(DynamicTransport.class);
        configuration = mock(ElasticsearchClientConfiguration.class);
        periodical = new NodeDiscoveryPeriodical(snifferAggregator, clientProvider, dynamicTransport, configuration);
    }

    @Test
    void doesNotSwapWhenNoNodesDiscovered() {
        when(snifferAggregator.sniff()).thenReturn(Collections.emptyList());

        periodical.doRun();

        verify(dynamicTransport, never()).swap(org.mockito.ArgumentMatchers.any());
        verify(clientProvider, never()).buildTransportForNodes(anyList());
    }

    @Test
    void swapsTransportWhenNodesChange() {
        final List<DiscoveredNode> nodes = List.of(
                new DiscoveredNode("https", "node1.example.com", 9200, Collections.emptyMap())
        );
        final OpenSearchTransport newTransport = mock(OpenSearchTransport.class);
        when(snifferAggregator.sniff()).thenReturn(nodes);
        when(clientProvider.buildTransportForNodes(nodes)).thenReturn(newTransport);

        periodical.doRun();

        verify(clientProvider).buildTransportForNodes(nodes);
        verify(dynamicTransport).swap(newTransport);
    }

    @Test
    void doesNotSwapWhenNodesUnchanged() {
        final List<DiscoveredNode> nodes = List.of(
                new DiscoveredNode("https", "node1.example.com", 9200, Collections.emptyMap())
        );
        final OpenSearchTransport newTransport = mock(OpenSearchTransport.class);
        when(snifferAggregator.sniff()).thenReturn(nodes);
        when(clientProvider.buildTransportForNodes(nodes)).thenReturn(newTransport);

        // First run triggers swap
        periodical.doRun();
        // Second run with same nodes does not
        periodical.doRun();

        verify(dynamicTransport, times(1)).swap(newTransport);
        verify(clientProvider, times(1)).buildTransportForNodes(nodes);
    }

    @Test
    void swapsAgainWhenNodesChangeAfterInitial() {
        final List<DiscoveredNode> firstNodes = List.of(
                new DiscoveredNode("https", "node1.example.com", 9200, Collections.emptyMap())
        );
        final List<DiscoveredNode> secondNodes = List.of(
                new DiscoveredNode("https", "node2.example.com", 9200, Collections.emptyMap())
        );
        final OpenSearchTransport transport1 = mock(OpenSearchTransport.class);
        final OpenSearchTransport transport2 = mock(OpenSearchTransport.class);
        when(snifferAggregator.sniff()).thenReturn(firstNodes).thenReturn(secondNodes);
        when(clientProvider.buildTransportForNodes(firstNodes)).thenReturn(transport1);
        when(clientProvider.buildTransportForNodes(secondNodes)).thenReturn(transport2);

        periodical.doRun();
        periodical.doRun();

        verify(dynamicTransport).swap(transport1);
        verify(dynamicTransport).swap(transport2);
    }

    @Test
    void startsOnThisNodeWhenDiscoveryEnabled() {
        when(configuration.discoveryEnabled()).thenReturn(true);
        when(configuration.isNodeActivityLogger()).thenReturn(false);
        assertThat(periodical.startOnThisNode()).isTrue();
    }

    @Test
    void startsOnThisNodeWhenNodeActivityLoggerEnabled() {
        when(configuration.discoveryEnabled()).thenReturn(false);
        when(configuration.isNodeActivityLogger()).thenReturn(true);
        assertThat(periodical.startOnThisNode()).isTrue();
    }

    @Test
    void doesNotStartWhenBothDisabled() {
        when(configuration.discoveryEnabled()).thenReturn(false);
        when(configuration.isNodeActivityLogger()).thenReturn(false);
        assertThat(periodical.startOnThisNode()).isFalse();
    }
}
