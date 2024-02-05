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
package org.graylog.datanode.management;

import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.ProcessState;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.ClusterClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.RequestOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.shaded.opensearch2.org.opensearch.common.settings.Settings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpensearchRemovalTracerTest {

    private OpensearchRemovalTracer classUnderTest;
    private final String NODENAME = "datanode1";
    @Mock
    private OpensearchProcess process;
    @Mock
    RestHighLevelClient restClient;
    @Mock
    ClusterClient clusterClient;

    @Before
    public void setUp() {
        when(process.restClient()).thenReturn(Optional.of(restClient));
        when(restClient.cluster()).thenReturn(clusterClient);
        this.classUnderTest = new OpensearchRemovalTracer(process, NODENAME);
    }

    @Test
    public void testResetAllocation() throws IOException {
        Settings settings = Settings.builder()
                .put(OpensearchRemovalTracer.CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING, NODENAME)
                .build();
        when(clusterClient.getSettings(any(), any())).thenReturn(new ClusterGetSettingsResponse(null, settings, null));
        classUnderTest.transition(ProcessEvent.HEALTH_CHECK_OK, ProcessState.STARTING, ProcessState.AVAILABLE);

        ArgumentCaptor<ClusterUpdateSettingsRequest> settingsRequest =
                ArgumentCaptor.forClass(ClusterUpdateSettingsRequest.class);
        verify(clusterClient).putSettings(settingsRequest.capture(), eq(RequestOptions.DEFAULT));
        assertNull(settingsRequest.getValue()
                .transientSettings()
                .get(OpensearchRemovalTracer.CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING)
        );
        assertTrue(classUnderTest.allocationExcludeChecked);
    }

    @Test
    public void testResetAllocationUnneccessary() throws IOException {
        Settings settings = Settings.builder()
                .put(OpensearchRemovalTracer.CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING, "notmynodename")
                .build();
        when(clusterClient.getSettings(any(), any())).thenReturn(new ClusterGetSettingsResponse(null, settings, null));
        classUnderTest.transition(ProcessEvent.HEALTH_CHECK_OK, ProcessState.STARTING, ProcessState.AVAILABLE);
        verify(clusterClient).getSettings(any(), any());
        verifyNoMoreInteractions(clusterClient);
        assertTrue(classUnderTest.allocationExcludeChecked);
    }

    @Test
    public void testRemovalTriggered() throws IOException {
        ClusterUpdateSettingsResponse response = mock(ClusterUpdateSettingsResponse.class);
        when(response.isAcknowledged()).thenReturn(true);
        when(clusterClient.putSettings(any(), any())).thenReturn(response);
        classUnderTest.transition(ProcessEvent.PROCESS_REMOVE, ProcessState.AVAILABLE, ProcessState.REMOVING);

        ArgumentCaptor<ClusterUpdateSettingsRequest> settingsRequest =
                ArgumentCaptor.forClass(ClusterUpdateSettingsRequest.class);
        verify(clusterClient).putSettings(settingsRequest.capture(), eq(RequestOptions.DEFAULT));
        assertEquals(NODENAME,
                settingsRequest.getValue()
                        .transientSettings()
                        .get(OpensearchRemovalTracer.CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING));
        assertFalse(classUnderTest.allocationExcludeChecked);
    }

    @Test
    public void testNoShutdownWhenRelocatingShards() throws IOException {
        ClusterHealthResponse health = mock(ClusterHealthResponse.class);
        when(health.getRelocatingShards()).thenReturn(5);
        when(clusterClient.health(any(), any())).thenReturn(health);
        classUnderTest.checkRemovalStatus();
        verify(process).restClient();
        verifyNoMoreInteractions(process);
    }

    @Test
    public void testShutdownWhenRemovedSuccessfully() throws IOException {
        ClusterHealthResponse health = mock(ClusterHealthResponse.class);
        when(health.getRelocatingShards()).thenReturn(0);
        when(clusterClient.health(any(), any())).thenReturn(health);
        final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        classUnderTest.executorService = executor;
        classUnderTest.checkRemovalStatus();
        verify(process).stop();
        verify(executor).shutdown();
    }


}
