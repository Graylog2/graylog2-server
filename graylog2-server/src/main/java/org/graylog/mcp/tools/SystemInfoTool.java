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

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog.mcp.server.Tool;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.shared.ServerVersion;
import org.graylog2.shared.security.RestPermissions;

import java.util.Locale;

public class SystemInfoTool extends Tool<SystemInfoTool.Parameters, String> {
    public static String NAME = "get_system_status";

    private final ServerStatus serverStatus;
    private final ClusterId clusterId;
    private final LeaderElectionService leaderElectionService;

    @Inject
    public SystemInfoTool(ObjectMapper objectMapper, ServerStatus serverStatus, ClusterConfigService clusterConfigService, LeaderElectionService leaderElectionService) {
        super(objectMapper,
                new TypeReference<>() {},
                NAME,
                "Get Graylog System Information",
                """
                        Returns system information about the Graylog installation, including
                        cluster ID, installed version, hostname, timezone, and operating system.
                        """);
        this.serverStatus = serverStatus;
        this.clusterId = clusterConfigService.getOrDefault(ClusterId.class, ClusterId.create(UUID.nilUUID().toString()));
        this.leaderElectionService = leaderElectionService;
    }

    @Override
    public String apply(PermissionHelper permissionHelper, SystemInfoTool.Parameters unused) {
        try {
            permissionHelper.checkPermission(RestPermissions.SYSTEM_READ, serverStatus.getNodeId().toString());

            return new ObjectMapper().writeValueAsString(SystemOverviewResponse.create("graylog-server",
                    ServerVersion.CODENAME,
                    serverStatus.getNodeId().toString(),
                    clusterId.clusterId(),
                    ServerVersion.VERSION.toString(),
                    Tools.getISO8601String(serverStatus.getStartedAt()),
                    serverStatus.isProcessing(),
                    Tools.getLocalCanonicalHostname(),
                    serverStatus.getLifecycle().getDescription().toLowerCase(Locale.ENGLISH),
                    serverStatus.getLifecycle().getLoadbalancerStatus().toString().toLowerCase(Locale.ENGLISH),
                    serverStatus.getTimezone().getID(),
                    System.getProperty("os.name", "unknown") + " " + System.getProperty("os.version", "unknown"),
                    leaderElectionService.isLeader()
            ));
        }
        catch (Exception e) {
            return e.getMessage();
        }
    }

    public static class Parameters {}
}
