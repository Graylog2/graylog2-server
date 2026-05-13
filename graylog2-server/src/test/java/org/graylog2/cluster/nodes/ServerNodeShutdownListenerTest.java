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
package org.graylog2.cluster.nodes;

import org.graylog2.cluster.nodes.mongodb.TestShutdownService;
import org.graylog2.plugin.lifecycles.Lifecycle;
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
class ServerNodeShutdownListenerTest {
    private static final String NODE_ID = "28164cbe-4ad9-4c9c-a76e-088655aa7889";

    private final NodeId nodeId = new SimpleNodeId(NODE_ID);

    @Mock
    private NodeService<ServerNodeDto> nodeService;

    private final TestShutdownService gracefulShutdownService = new TestShutdownService();


    @BeforeEach
    void setUp() {
        new ServerNodeShutdownListener(gracefulShutdownService, nodeService, nodeId);
    }

    @Test
    void haltingLifecycleMarksCurrentNodeOffline() {
        final ServerNodeDto current = ServerNodeDto.Builder.builder()
                .setId(NODE_ID)
                .setLeader(true)
                .setHostname("host.example.com")
                .setProcessing(true)
                .setLifecycle(Lifecycle.RUNNING)
                .setVersion("6.3.0")
                .build();
        when(nodeService.byNodeIdAnyState(NODE_ID)).thenReturn(Optional.of(current));

        gracefulShutdownService.shutDown();

        final ArgumentCaptor<ServerNodeDto> captor = ArgumentCaptor.forClass(ServerNodeDto.class);
        verify(nodeService).update(captor.capture());

        final ServerNodeDto written = captor.getValue();
        assertThat(written.isOnline()).isFalse();
        assertThat(written.isLeader()).isFalse();
        // Inventory fields must be preserved so the row remains useful for upgrade tracking.
        assertThat(written.getVersion()).isEqualTo("6.3.0");
        assertThat(written.getLifecycle()).isEqualTo(Lifecycle.RUNNING);
        assertThat(written.getHostname()).isEqualTo("host.example.com");
    }


    @Test
    void missingNodeIsLoggedAndDoesNotThrow() {
        when(nodeService.byNodeIdAnyState(NODE_ID)).thenReturn(Optional.empty());

        // Must not throw — shutdown should not be blocked by a missing inventory row.
        gracefulShutdownService.shutDown();

        verify(nodeService, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unexpectedFailureIsSwallowed() {
        when(nodeService.byNodeIdAnyState(NODE_ID))
                .thenThrow(new RuntimeException("mongo unreachable"));

        // Must not propagate — failure during shutdown shouldn't block the JVM exit.
        gracefulShutdownService.shutDown();

        verify(nodeService, never()).update(org.mockito.ArgumentMatchers.any());
    }
}
