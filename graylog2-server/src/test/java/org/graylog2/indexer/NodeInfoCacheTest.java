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
package org.graylog2.indexer;

import org.graylog2.indexer.cluster.Cluster;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NodeInfoCacheTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Cluster cluster;
    private NodeInfoCache nodeInfoCache;

    @Before
    public void setUp() {
        nodeInfoCache = new NodeInfoCache(cluster);
    }

    @Test
    public void getNodeNameReturnsNodeNameIfNodeIdIsValid() {
        when(cluster.nodeIdToName("node_id")).thenReturn(Optional.of("Node Name"));
        assertThat(nodeInfoCache.getNodeName("node_id")).contains("Node Name");
    }

    @Test
    public void getNodeNameUsesCache() {
        when(cluster.nodeIdToName("node_id")).thenReturn(Optional.of("Node Name"));

        nodeInfoCache.getNodeName("node_id");
        nodeInfoCache.getNodeName("node_id");

        verify(cluster, times(1)).nodeIdToName("node_id");
    }

    @Test
    public void getNodeNameReturnsEmptyOptionalIfNodeIdIsInvalid() {
        when(cluster.nodeIdToName("node_id")).thenReturn(Optional.empty());
        assertThat(nodeInfoCache.getNodeName("node_id")).isEmpty();
    }

    @Test
    public void getHostNameReturnsNodeNameIfNodeIdIsValid() {
        when(cluster.nodeIdToHostName("node_id")).thenReturn(Optional.of("node-hostname"));
        assertThat(nodeInfoCache.getHostName("node_id")).contains("node-hostname");
    }

    @Test
    public void getHostNameUsesCache() {
        when(cluster.nodeIdToHostName("node_id")).thenReturn(Optional.of("node-hostname"));

        nodeInfoCache.getHostName("node_id");
        nodeInfoCache.getHostName("node_id");

        verify(cluster, times(1)).nodeIdToHostName("node_id");
    }

    @Test
    public void getHostNameReturnsEmptyOptionalIfNodeIdIsInvalid() {
        when(cluster.nodeIdToHostName("node_id")).thenReturn(Optional.empty());
        assertThat(nodeInfoCache.getHostName("node_id")).isEmpty();
    }
}