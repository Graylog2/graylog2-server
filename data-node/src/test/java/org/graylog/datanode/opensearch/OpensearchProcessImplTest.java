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
package org.graylog.datanode.opensearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.opensearch.statemachine.OpensearchStateMachine;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.ClusterClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.RequestOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.shaded.opensearch2.org.opensearch.common.settings.Settings;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.security.CustomCAX509TrustManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpensearchProcessImplTest {

    OpensearchProcessImpl opensearchProcess;
    @Mock
    private DatanodeConfiguration datanodeConfiguration;
    @Mock
    private CustomCAX509TrustManager trustmManager;
    @Mock
    private Configuration configuration;
    @Mock
    private NodeService<DataNodeDto> nodeService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private OpensearchStateMachine processState;
    private final String nodeName = "test-node";
    private final NodeId nodeId = new SimpleNodeId(nodeName);
    @Mock
    private EventBus eventBus;

    @Mock
    RestHighLevelClient restClient;
    @Mock
    ClusterClient clusterClient;

    @Before
    public void setup() throws IOException {
        when(datanodeConfiguration.processLogsBufferSize()).thenReturn(100);
        this.opensearchProcess = spy(new OpensearchProcessImpl(datanodeConfiguration, trustmManager, configuration,
                nodeService, objectMapper, processState, nodeName, nodeId, eventBus));
        when(opensearchProcess.restClient()).thenReturn(Optional.of(restClient));
        when(restClient.cluster()).thenReturn(clusterClient);
    }


    @Test
    public void testResetAllocation() throws IOException {
        Settings settings = Settings.builder()
                .put(OpensearchProcessImpl.CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING, nodeName)
                .build();
        when(clusterClient.getSettings(any(), any())).thenReturn(new ClusterGetSettingsResponse(null, settings, null));
        opensearchProcess.available();

        ArgumentCaptor<ClusterUpdateSettingsRequest> settingsRequest =
                ArgumentCaptor.forClass(ClusterUpdateSettingsRequest.class);
        verify(clusterClient).putSettings(settingsRequest.capture(), eq(RequestOptions.DEFAULT));
        assertNull(settingsRequest.getValue()
                .transientSettings()
                .get(OpensearchProcessImpl.CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING)
        );
        assertTrue(opensearchProcess.allocationExcludeChecked);
    }

    @Test
    public void testResetAllocationUnneccessary() throws IOException {
        Settings settings = Settings.builder()
                .put(OpensearchProcessImpl.CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING, "notmynodename")
                .build();
        when(clusterClient.getSettings(any(), any())).thenReturn(new ClusterGetSettingsResponse(null, settings, null));
        opensearchProcess.available();
        verify(clusterClient).getSettings(any(), any());
        verifyNoMoreInteractions(clusterClient);
        assertTrue(opensearchProcess.allocationExcludeChecked);
    }

    @Test
    public void testShutdownWhenRemovedSuccessfully() throws IOException {
        ClusterHealthResponse health = mock(ClusterHealthResponse.class);
        when(health.getRelocatingShards()).thenReturn(0);
        when(clusterClient.health(any(), any())).thenReturn(health);
        final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        opensearchProcess.executorService = executor;
        opensearchProcess.checkRemovalStatus();
        verify(opensearchProcess).stop();
        verify(executor).shutdown();
    }

}
