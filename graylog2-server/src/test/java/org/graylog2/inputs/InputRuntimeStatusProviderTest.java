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
package org.graylog2.inputs;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.resources.system.inputs.RemoteInputStatesResource;
import org.graylog2.shared.inputs.InputRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InputRuntimeStatusProviderTest {

    private static final String LOCAL_NODE_ID = "local-node-1";
    private static final String AUTH_TOKEN = "test-token";

    private InputRuntimeStatusProvider provider;

    @Mock
    private NodeService nodeService;

    @Mock
    private RemoteInterfaceProvider remoteInterfaceProvider;

    @Mock
    private InputRegistry inputRegistry;

    @Mock
    private NodeId nodeId;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder()
                .setNameFormat("input-runtime-status-test-%d")
                .build());
        lenient().when(nodeId.getNodeId()).thenReturn(LOCAL_NODE_ID);

        provider = new InputRuntimeStatusProvider(
                nodeService,
                remoteInterfaceProvider,
                inputRegistry,
                nodeId,
                executorService
        );
    }

    @Test
    void returnsFieldNameCorrectly() {
        assertEquals("runtime_status", provider.getFieldName());
    }

    @Test
    void returnsEmptySetForInvalidStatus() {
        final Set<String> result = provider.getMatchingIds("NOT_A_VALID_STATUS", AUTH_TOKEN);
        assertTrue(result.isEmpty());
    }

    @Test
    void localRegistryUsedDirectly() {
        when(inputRegistry.getStatusesByInputId()).thenReturn(Map.of(
                "input-1", "RUNNING",
                "input-2", "FAILED"
        ));
        when(nodeService.allActive()).thenReturn(Map.of(LOCAL_NODE_ID, mock(Node.class)));

        // Should find the running input without any remote calls
        final Set<String> running = provider.getMatchingIds("RUNNING", AUTH_TOKEN);
        assertEquals(Set.of("input-1"), running);

        // Verify no remote HTTP calls were made (local node is skipped)
        verify(remoteInterfaceProvider, never()).get(any(Node.class), any(), eq(RemoteInputStatesResource.class));
    }

    @Test
    void remoteNodesQueriedInParallel() throws Exception {
        when(inputRegistry.getStatusesByInputId()).thenReturn(Map.of());

        // Set up two remote nodes
        Node remoteNode1 = mock(Node.class);
        Node remoteNode2 = mock(Node.class);

        when(nodeService.allActive()).thenReturn(Map.of(
                "remote-1", remoteNode1,
                "remote-2", remoteNode2
        ));

        // Mock remote calls
        mockRemoteCall(remoteNode1, Map.of("input-a", "RUNNING", "input-b", "FAILED"));
        mockRemoteCall(remoteNode2, Map.of("input-c", "RUNNING"));

        final Set<String> running = provider.getMatchingIds("RUNNING", AUTH_TOKEN);
        assertEquals(Set.of("input-a", "input-c"), running);
    }

    @Test
    void globalInputMatchesAnyNodeStatus() throws Exception {
        // Same input on local node is RUNNING, on remote node is FAILED
        when(inputRegistry.getStatusesByInputId())
                .thenReturn(Map.of("global-input", "RUNNING"));

        Node remoteNode = mock(Node.class);
        when(nodeService.allActive()).thenReturn(Map.of(
                LOCAL_NODE_ID, mock(Node.class),
                "remote-1", remoteNode
        ));

        mockRemoteCall(remoteNode, Map.of("global-input", "FAILED"));

        // Create a new provider so cache is fresh
        provider = new InputRuntimeStatusProvider(
                nodeService, remoteInterfaceProvider, inputRegistry, nodeId, executorService);

        // Should match RUNNING (from local) — cache has both RUNNING and FAILED for global-input
        final Set<String> running = provider.getMatchingIds("RUNNING", AUTH_TOKEN);
        assertTrue(running.contains("global-input"));

        // Should also match FAILED (from remote) using same cache
        final Set<String> failed = provider.getMatchingIds("FAILED", AUTH_TOKEN);
        assertTrue(failed.contains("global-input"));
    }

    @Test
    void cacheReuseAvoidsDuplicateFetch() {
        when(inputRegistry.getStatusesByInputId()).thenReturn(Map.of("input-1", "RUNNING"));
        when(nodeService.allActive()).thenReturn(Map.of(LOCAL_NODE_ID, mock(Node.class)));

        // First call populates cache
        provider.getMatchingIds("RUNNING", AUTH_TOKEN);
        // Second call should reuse cache
        provider.getMatchingIds("RUNNING", AUTH_TOKEN);

        // getStatusesByInputId() should only be called once (cached on second call)
        verify(inputRegistry, times(1)).getStatusesByInputId();
    }

    @Test
    void nodeFailureHandledGracefully() throws Exception {
        when(inputRegistry.getStatusesByInputId()).thenReturn(Map.of());

        Node failingNode = mock(Node.class);
        Node goodNode = mock(Node.class);

        when(nodeService.allActive()).thenReturn(Map.of(
                "failing-node", failingNode,
                "good-node", goodNode
        ));

        // Failing node throws IOException
        RemoteInputStatesResource failingRemote = mock(RemoteInputStatesResource.class);
        @SuppressWarnings("unchecked")
        Call<Map<String, String>> failingCall = mock(Call.class);
        when(failingCall.execute()).thenThrow(new IOException("Connection refused"));
        when(failingRemote.getLocalStatuses()).thenReturn(failingCall);
        when(remoteInterfaceProvider.get(failingNode, AUTH_TOKEN, RemoteInputStatesResource.class))
                .thenReturn(failingRemote);

        // Good node returns data
        mockRemoteCall(goodNode, Map.of("input-1", "RUNNING"));

        final Set<String> running = provider.getMatchingIds("RUNNING", AUTH_TOKEN);
        assertEquals(Set.of("input-1"), running);
    }

    @Test
    void handlesUpperCaseAndLowerCaseStatus() {
        // Invalid after uppercase conversion should return empty
        assertTrue(provider.getMatchingIds("invalid", AUTH_TOKEN).isEmpty());
        assertTrue(provider.getMatchingIds("INVALID", AUTH_TOKEN).isEmpty());
    }

    @Test
    void failedGroupMatchesFailingAndInvalidConfiguration() {
        when(inputRegistry.getStatusesByInputId()).thenReturn(Map.of(
                "input-1", "FAILING",
                "input-2", "INVALID_CONFIGURATION",
                "input-3", "RUNNING",
                "input-4", "FAILED"
        ));
        when(nodeService.allActive()).thenReturn(Map.of(LOCAL_NODE_ID, mock(Node.class)));

        final Set<String> failed = provider.getMatchingIds("FAILED", AUTH_TOKEN);
        assertEquals(Set.of("input-1", "input-2", "input-4"), failed);
    }

    @Test
    void notRunningGroupMatchesStoppedTerminatedStoppingCreated() throws Exception {
        when(inputRegistry.getStatusesByInputId()).thenReturn(Map.of(
                "input-1", "STOPPED",
                "input-2", "TERMINATED",
                "input-3", "RUNNING",
                "input-4", "CREATED"
        ));

        Node remoteNode = mock(Node.class);
        when(nodeService.allActive()).thenReturn(Map.of(
                LOCAL_NODE_ID, mock(Node.class),
                "remote-1", remoteNode
        ));
        mockRemoteCall(remoteNode, Map.of("input-5", "STOPPING"));

        final Set<String> notRunning = provider.getMatchingIds("NOT_RUNNING", AUTH_TOKEN);
        assertEquals(Set.of("input-1", "input-2", "input-4", "input-5"), notRunning);
    }

    @Test
    void setupGroupMatchesSetupInitializedStarting() {
        when(inputRegistry.getStatusesByInputId()).thenReturn(Map.of(
                "input-1", "SETUP",
                "input-2", "INITIALIZED",
                "input-3", "STARTING",
                "input-4", "RUNNING"
        ));
        when(nodeService.allActive()).thenReturn(Map.of(LOCAL_NODE_ID, mock(Node.class)));

        final Set<String> setup = provider.getMatchingIds("SETUP", AUTH_TOKEN);
        assertEquals(Set.of("input-1", "input-2", "input-3"), setup);
    }

    @SuppressWarnings("unchecked")
    private void mockRemoteCall(Node node, Map<String, String> statuses) throws IOException {
        RemoteInputStatesResource remote = mock(RemoteInputStatesResource.class);
        Call<Map<String, String>> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(statuses));
        when(remote.getLocalStatuses()).thenReturn(call);
        when(remoteInterfaceProvider.get(node, AUTH_TOKEN, RemoteInputStatesResource.class))
                .thenReturn(remote);
    }
}
