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

import com.google.common.eventbus.EventBus;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.cluster.nodes.TestDataNodeNodeClusterService;
import org.graylog2.events.ClusterEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class DataNodeCommandServiceImplTest {

    @Mock
    private ClusterEventBus clusterEventBus;
    @Mock
    private EventBus eventBus;
    private NodeService<DataNodeDto> nodeService;

    private DataNodeCommandServiceImpl classUnderTest;

    @Before
    public void setUp() {
        this.nodeService = new TestDataNodeNodeClusterService();
        this.classUnderTest = new DataNodeCommandServiceImpl(clusterEventBus, nodeService, eventBus);
    }

    private DataNodeDto buildTestNode(String nodeId, DataNodeStatus status) {
        return DataNodeDto.Builder.builder()
                .setId(nodeId)
                .setHostname("localhost")
                .setClusterAddress("http://localhost:9300")
                .setTransportAddress("http://localhost:9200")
                .setLeader(true)
                .setDataNodeStatus(status)
                .build();
    }

    @Test
    public void removeNodeFailsForLastNode() throws NodeNotFoundException {
        final String testNodeId = "node";
        nodeService.registerServer(buildTestNode(testNodeId, DataNodeStatus.AVAILABLE));

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            classUnderTest.removeNode(testNodeId);
        });
        assertEquals("Cannot remove last data node in the cluster.", e.getMessage());
        verifyNoMoreInteractions(clusterEventBus);
    }

    @Test
    public void removeNodeFailsWhenRemovingAllSequentially() throws NodeNotFoundException {
        final String testNodeId = "node";
        nodeService.registerServer(buildTestNode(testNodeId, DataNodeStatus.AVAILABLE));
        nodeService.registerServer(buildTestNode("othernode", DataNodeStatus.REMOVING));

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            classUnderTest.removeNode(testNodeId);
        });
        assertEquals("Cannot remove last data node in the cluster.", e.getMessage());
        verifyNoMoreInteractions(clusterEventBus);
    }

    @Test
    public void removeNodesPostsFirstToEventBus() throws NodeNotFoundException {
        String node1 = "node1";
        String node2 = "node2";
        nodeService.registerServer(buildTestNode(node1, DataNodeStatus.AVAILABLE));
        nodeService.registerServer(buildTestNode(node2, DataNodeStatus.AVAILABLE));
        nodeService.registerServer(buildTestNode("node3", DataNodeStatus.AVAILABLE));

        classUnderTest.removeNode(node1);
        verify(clusterEventBus).post(DataNodeLifecycleEvent.create(node1, DataNodeLifecycleTrigger.REMOVE));

        classUnderTest.removeNode(node2);
        verifyNoMoreInteractions(clusterEventBus);

        long removeCount = nodeService.allActive().values().stream()
                .filter(dto -> dto.getActionQueue() == DataNodeLifecycleTrigger.REMOVE)
                .count();

        assertEquals(removeCount, 2);
    }

    @Test
    public void removeNodePublishesClusterEvent() throws NodeNotFoundException {
        final String testNodeId = "node";
        nodeService.registerServer(buildTestNode(testNodeId, DataNodeStatus.AVAILABLE));
        nodeService.registerServer(buildTestNode("othernode", DataNodeStatus.AVAILABLE));

        classUnderTest.removeNode(testNodeId);
        verify(clusterEventBus).post(DataNodeLifecycleEvent.create(testNodeId, DataNodeLifecycleTrigger.REMOVE));
    }

    @Test
    public void resetNodeFailsWhenNodeNotRemoved() throws NodeNotFoundException {
        final String testNodeId = "node";
        nodeService.registerServer(buildTestNode(testNodeId, DataNodeStatus.AVAILABLE));
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            classUnderTest.resetNode(testNodeId);
        });
        assertEquals("Only previously removed data nodes can rejoin the cluster.", e.getMessage());
        verifyNoMoreInteractions(clusterEventBus);
    }

    @Test
    public void resetNodePublishesClusterEvent() throws NodeNotFoundException {
        final String testNodeId = "node";
        nodeService.registerServer(buildTestNode(testNodeId, DataNodeStatus.REMOVED));
        classUnderTest.resetNode(testNodeId);
        verify(clusterEventBus).post(DataNodeLifecycleEvent.create(testNodeId, DataNodeLifecycleTrigger.RESET));
    }

    @Test
    public void stopNodeFailsWhenNodeNotAvailable() throws NodeNotFoundException {
        final String testNodeId = "node";
        nodeService.registerServer(buildTestNode(testNodeId, DataNodeStatus.REMOVED));
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            classUnderTest.stopNode(testNodeId);
        });
        assertEquals("Only running data nodes can be stopped.", e.getMessage());
        verifyNoMoreInteractions(clusterEventBus);
    }

    @Test
    public void stopNodePublishesClusterEvent() throws NodeNotFoundException {
        final String testNodeId = "node";
        nodeService.registerServer(buildTestNode(testNodeId, DataNodeStatus.AVAILABLE));
        classUnderTest.stopNode(testNodeId);
        verify(clusterEventBus).post(DataNodeLifecycleEvent.create(testNodeId, DataNodeLifecycleTrigger.STOP));
    }

    @Test
    public void startNodeFailsWhenNodeNotStopped() throws NodeNotFoundException {
        final String testNodeId = "node";
        nodeService.registerServer(buildTestNode(testNodeId, DataNodeStatus.AVAILABLE));
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            classUnderTest.startNode(testNodeId);
        });
        assertEquals("Only stopped data nodes can be started.", e.getMessage());
        verifyNoMoreInteractions(clusterEventBus);
    }

    @Test
    public void startNodePublishesClusterEvent() throws NodeNotFoundException {
        final String testNodeId = "node";
        nodeService.registerServer(buildTestNode(testNodeId, DataNodeStatus.UNAVAILABLE));
        classUnderTest.startNode(testNodeId);
        verify(clusterEventBus).post(DataNodeLifecycleEvent.create(testNodeId, DataNodeLifecycleTrigger.START));
    }

    @Test
    public void removedLifecycleEventRemovesNextNode() {
        DataNodeDto node1 = buildTestNode("node1", DataNodeStatus.REMOVING);
        nodeService.registerServer(node1);
        DataNodeDto node2 = buildTestNode("node2", DataNodeStatus.AVAILABLE);
        nodeService.registerServer(node2);
        DataNodeDto node3 = buildTestNode("node3", DataNodeStatus.AVAILABLE);
        nodeService.registerServer(node3);

        nodeService.update(node2.toBuilder().setActionQueue(DataNodeLifecycleTrigger.REMOVE).build());
        nodeService.update(node3.toBuilder().setActionQueue(DataNodeLifecycleTrigger.REMOVE).build());

        classUnderTest.handleDataNodeLifeCycleEvent(DataNodeLifecycleEvent.create("node1", DataNodeLifecycleTrigger.REMOVED));

        verify(clusterEventBus, times(1)).post(any());

    }

}
