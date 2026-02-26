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
import org.assertj.core.api.Assertions;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchStateMachine;
import org.graylog.storage.opensearch3.ClusterAdapterOS;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog2.datanode.DataNodeNotficationEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.rest.models.system.indexer.responses.ClusterHealth;
import org.graylog2.security.CustomCAX509TrustManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import oshi.hardware.GlobalMemory;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class OpensearchProcessImplTest {

    OpensearchProcessImpl opensearchProcess;
    @Mock
    private DatanodeConfiguration datanodeConfiguration;
    @Mock
    private CustomCAX509TrustManager trustmManager;
    @Mock
    private Configuration configuration;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private OpensearchStateMachine processState;
    private final String nodeName = "test-node";
    private final NodeId nodeId = new SimpleNodeId(nodeName);
    @Mock
    private EventBus eventBus;
    @Mock
    private ClusterAdapterOS clusterAdapter;
    @Mock
    private OfficialOpensearchClient client;

    @Mock
    ClusterEventBus clusterEventBus;

    @BeforeEach
    public void setup() throws IOException {
        when(datanodeConfiguration.processLogsBufferSize()).thenReturn(100);
        when(configuration.getDatanodeNodeName()).thenReturn(nodeName);
        this.opensearchProcess = spy(new OpensearchProcessImpl(datanodeConfiguration, trustmManager, configuration,
                objectMapper, processState, nodeId, eventBus, clusterEventBus));
        when(opensearchProcess.openSearchClient()).thenReturn(Optional.of(client));
        when(opensearchProcess.clusterAdapter()).thenReturn(clusterAdapter);
    }


    @Test
    public void testResetAllocation() {
        when(clusterAdapter.getClusterSetting(OpensearchProcessImpl.CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING))
                .thenReturn(nodeName);
        opensearchProcess.available();

        verify(clusterAdapter).updateClusterSetting(
                OpensearchProcessImpl.CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING,
                null,
                false
        );

        assertTrue(opensearchProcess.allocationExcludeChecked);
    }

    @Test
    public void testResetAllocationUnneccessary() throws IOException {
        when(clusterAdapter.getClusterSetting(OpensearchProcessImpl.CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING))
                .thenReturn("notMyNodeName");
        opensearchProcess.available();
        opensearchProcess.available();
        verify(clusterAdapter).getClusterSetting(any());
        verifyNoMoreInteractions(clusterAdapter);
        assertTrue(opensearchProcess.allocationExcludeChecked);
    }

    @Test
    public void testShutdownWhenRemovedSuccessfully() throws IOException {
        ClusterHealth health = ClusterHealth.create("green",
                ClusterHealth.ShardStatus.create(
                        1, 0, 0, 0
                )); // relocating shards  are important for the removal
        when(clusterAdapter.clusterHealthStats()).thenReturn(Optional.of(health));

        final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        opensearchProcess.checkRemovalStatus(executor);
        verify(processState).fire(OpensearchEvent.PROCESS_STOPPED);
        verify(executor).shutdown();
    }

    @Test
    public void testHeapThresholdWarning() {
        when(configuration.getHostname()).thenReturn("datanode");
        when(configuration.getOpensearchHeap()).thenReturn("1g");
        when(opensearchProcess.getGlobalMemory()).thenReturn(mockMemory(gigabytes(8), gigabytes(16)));
        opensearchProcess.checkConfiguredHeap();
        verify(clusterEventBus, times(1)).post(any(DataNodeNotficationEvent.class));
    }

    @Test
    public void testNoHeapThresholdWarning() {
        when(configuration.getOpensearchHeap()).thenReturn("1g");
        when(opensearchProcess.getGlobalMemory()).thenReturn(mockMemory(gigabytes(2), gigabytes(3)));
        opensearchProcess.checkConfiguredHeap();
        verifyNoInteractions(clusterEventBus);
    }

    private GlobalMemory mockMemory(long availableMemory, long totalMemory) {
        return new GlobalMemory() {

            @Override
            public long getTotal() {
                return totalMemory;
            }

            @Override
            public long getAvailable() {
                return availableMemory;
            }

            @Override
            public long getPageSize() {
                throw new UnsupportedOperationException("Not supported here");
            }

            @Override
            public VirtualMemory getVirtualMemory() {
                throw new UnsupportedOperationException("Not supported here");
            }

            @Override
            public List<PhysicalMemory> getPhysicalMemory() {
                throw new UnsupportedOperationException("Not supported here");
            }
        };
    }

    private static long gigabytes(int i) {
        return i * 1024 * 1024 * 1024L;
    }

    @Test
    public void recommendedMemorySettingValue() {
        Assertions.assertThat(OpensearchProcessImpl.recommendedMemorySetting("7 GB"))
                .isEqualTo("7g");

        Assertions.assertThat(OpensearchProcessImpl.recommendedMemorySetting("512 MB"))
                .isEqualTo("512m");
    }
}
