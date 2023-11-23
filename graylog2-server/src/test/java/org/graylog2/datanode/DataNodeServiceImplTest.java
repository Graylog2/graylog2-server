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
package org.graylog2.datanode;

import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.events.ClusterEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DataNodeServiceImplTest {

    @Mock
    private ClusterEventBus clusterEventBus;
    @Mock
    private NodeService nodeService;

    private DataNodeServiceImpl classUnderTest;

    @Before
    public void setUp() {
        this.classUnderTest = new DataNodeServiceImpl(clusterEventBus, nodeService);
    }

    @Test
    public void removeNodePublishesClusterEvent() throws NodeNotFoundException {
        Node node = mock(Node.class);
        when(node.getNodeId()).thenReturn("returnedid"); // would be the same in real life
        when(nodeService.byNodeId("nodeid")).thenReturn(node);
        classUnderTest.removeNode("nodeid");
        verify(clusterEventBus).post(DataNodeLifecycleEvent.create(node.getNodeId(), DataNodeLifecycleTrigger.REMOVE));
    }

    @Test
    public void resetNodePublishesClusterEvent() throws NodeNotFoundException {
        Node node = mock(Node.class);
        when(node.getNodeId()).thenReturn("returnedid"); // would be the same in real life
        when(nodeService.byNodeId("nodeid")).thenReturn(node);
        classUnderTest.resetNode("nodeid");
        verify(clusterEventBus).post(DataNodeLifecycleEvent.create(node.getNodeId(), DataNodeLifecycleTrigger.RESET));
    }

}
