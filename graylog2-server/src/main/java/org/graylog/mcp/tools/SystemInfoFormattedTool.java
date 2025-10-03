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
import org.graylog.mcp.server.OutputBuilder;
import org.graylog.mcp.server.Tool;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.shared.ServerVersion;
import org.graylog2.shared.security.RestPermissions;

import java.util.List;
import java.util.Locale;

public class SystemInfoFormattedTool extends Tool<SystemInfoFormattedTool.Parameters, String> {
    public static String NAME = "get_formatted_system_status";

    private final ServerStatus serverStatus;
    private final ClusterId clusterId;
    private final LeaderElectionService leaderElectionService;

    @Inject
    public SystemInfoFormattedTool(ObjectMapper objectMapper, ServerStatus serverStatus, ClusterConfigService clusterConfigService, LeaderElectionService leaderElectionService) {
        super(objectMapper,
                new TypeReference<>() {},
                new TypeReference<>() {},
                NAME,
                "Get Graylog System Information",
                """
                        Returns Markdown containing system information about the Graylog installation, including
                        cluster ID, installed version, hostname, timezone, and operating system.
                        """);
        this.serverStatus = serverStatus;
        this.clusterId = clusterConfigService.getOrDefault(ClusterId.class, ClusterId.create(UUID.nilUUID().toString()));
        this.leaderElectionService = leaderElectionService;
    }

    @Override
    public String apply(PermissionHelper permissionHelper, SystemInfoFormattedTool.Parameters unused) {
        try {
            // TODO: find a better way to do this. This is all verbatim from org.graylog2.shared.rest.resources.system.SystemResource::system

            permissionHelper.checkPermission(RestPermissions.SYSTEM_READ, serverStatus.getNodeId().toString());

            OutputBuilder output = new OutputBuilder();
            output.h1("Graylog System Information");
            return output.unorderedListKVItem(SystemOverviewResponse.create("graylog-server",
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
            ), List.of(
                    "facility",
                    "codename",
                    "node_id",
                    "cluster_id",
                    "version",
                    "started_at",
                    "is_processing",
                    "hostname",
                    "lifecycle",
                    "lb_status",
                    "timezone",
                    "operating_system",
                    "is_leader"
            )).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class Parameters {}
}
