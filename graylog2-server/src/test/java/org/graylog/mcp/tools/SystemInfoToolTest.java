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
package org.graylog.mcp.tools;

import org.graylog.mcp.server.SchemaGeneratorProvider;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.web.customization.CustomizationConfig;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemInfoToolTest {
    private static final String CLUSTER_ID = "7c7c3b7e-6a0d-4f0b-9f0e-2b7c1a3c9d11";

    private final PermissionHelper permissionHelper = mock(PermissionHelper.class);
    private final ClusterConfigService clusterConfigService = mock(ClusterConfigService.class);
    private final ServerStatus serverStatus = mock(ServerStatus.class);
    private final LeaderElectionService leaderElectionService = mock(LeaderElectionService.class);
    private final CustomizationConfig customizationConfig = new CustomizationConfig(null);

    @BeforeEach
    void setUp() {
        final NodeId nodeId = mock(NodeId.class);
        when(nodeId.getNodeId()).thenReturn("node-1");
        when(nodeId.toString()).thenReturn("node-1");
        when(serverStatus.getNodeId()).thenReturn(nodeId);
        // ServerStatus#getClusterId() is always empty; the real cluster ID lives in the cluster config.
        when(serverStatus.getClusterId()).thenReturn("");
        when(serverStatus.getStartedAt()).thenReturn(DateTime.now(DateTimeZone.UTC));
        when(serverStatus.getLifecycle()).thenReturn(Lifecycle.RUNNING);
        when(serverStatus.getTimezone()).thenReturn(DateTimeZone.UTC);
        when(clusterConfigService.getOrDefault(eq(ClusterId.class), any(ClusterId.class)))
                .thenReturn(ClusterId.create(CLUSTER_ID));
    }

    @Test
    void systemInfoToolReportsClusterIdFromClusterConfig() {
        final var tool = new SystemInfoTool(serverStatus, leaderElectionService, customizationConfig,
                new ObjectMapperProvider().get(), clusterConfigService, new SchemaGeneratorProvider(Set.of()));

        assertThat(tool.apply(permissionHelper, new SystemInfoTool.Parameters()).clusterId()).isEqualTo(CLUSTER_ID);
    }

    @Test
    void formattedSystemInfoToolReportsClusterIdFromClusterConfig() {
        final var tool = new SystemInfoFormattedTool(new ObjectMapperProvider().get(),
                new SchemaGeneratorProvider(Set.of()), customizationConfig, serverStatus, leaderElectionService,
                clusterConfigService);

        assertThat(tool.apply(permissionHelper, new SystemInfoFormattedTool.Parameters())).contains(CLUSTER_ID);
    }
}
