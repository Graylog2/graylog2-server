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
package org.graylog.datanode.shutdown;

import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataNodeShutdownListenerTest {
    private static final String NODE_ID = "28164cbe-4ad9-4c9c-a76e-088655aa7889";

    private final NodeId nodeId = new SimpleNodeId(NODE_ID);

    @Mock
    private GracefulShutdownService shutdownService;

    @Mock
    private NodeService<DataNodeDto> nodeService;

    private DataNodeShutdownListener listener;

    @BeforeEach
    void setUp() {
        listener = new DataNodeShutdownListener(shutdownService, nodeService, nodeId);
    }

    @Test
    void registersItselfAsGracefulShutdownHook() {
        verify(shutdownService).register(listener);
    }

    @Test
    void gracefulShutdownMarksCurrentNodeOffline() throws Exception {
        final DataNodeDto current = DataNodeDto.Builder.builder()
                .setId(NODE_ID)
                .setLeader(true)
                .setHostname("datanode.example.com")
                .setDataNodeStatus(DataNodeStatus.AVAILABLE)
                .setDatanodeVersion("6.3.0")
                .build();
        when(nodeService.byNodeIdAnyState(NODE_ID)).thenReturn(Optional.of(current));

        listener.doGracefulShutdown();

        final ArgumentCaptor<DataNodeDto> captor = ArgumentCaptor.forClass(DataNodeDto.class);
        verify(nodeService).update(captor.capture());

        final DataNodeDto written = captor.getValue();
        assertThat(written.isOnline()).isFalse();
        assertThat(written.isLeader()).isFalse();
        // Inventory fields must be preserved so the row remains useful for upgrade tracking.
        assertThat(written.getDatanodeVersion()).isEqualTo("6.3.0");
        assertThat(written.getDataNodeStatus()).isEqualTo(DataNodeStatus.AVAILABLE);
        assertThat(written.getHostname()).isEqualTo("datanode.example.com");
    }

    @Test
    void missingNodeIsLoggedAndDoesNotThrow() throws Exception {
        when(nodeService.byNodeIdAnyState(NODE_ID)).thenReturn(Optional.empty());

        // Must not throw — shutdown should not be blocked by a missing inventory row.
        listener.doGracefulShutdown();

        verify(nodeService, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unexpectedFailureIsSwallowed() throws Exception {
        when(nodeService.byNodeIdAnyState(NODE_ID))
                .thenThrow(new RuntimeException("mongo unreachable"));

        // Must not propagate — failure during shutdown shouldn't block the JVM exit.
        listener.doGracefulShutdown();

        verify(nodeService, never()).update(org.mockito.ArgumentMatchers.any());
    }
}
