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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog.mcp.server.SchemaGeneratorProvider;
import org.graylog.mcp.server.Tool;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.shared.ServerVersion;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.web.customization.CustomizationConfig;

import java.util.Locale;

import static org.graylog2.shared.utilities.StringUtils.f;

public class SystemInfoTool extends Tool<SystemInfoTool.Parameters, SystemOverviewResponse> {
    public static String NAME = "get_system_status";

    private final ServerStatus serverStatus;
    private final LeaderElectionService leaderElectionService;
    private final CustomizationConfig customizationConfig;

    @Inject
    public SystemInfoTool(ServerStatus serverStatus,
                          LeaderElectionService leaderElectionService,
                          final CustomizationConfig customizationConfig,
                          final ObjectMapper objectMapper,
                          final ClusterConfigService clusterConfigService,
                          final SchemaGeneratorProvider schemaGeneratorProvider) {
        super(
                new TypeReference<>() {},
                new TypeReference<>() {},
                NAME,
                f("Get %s System Information", customizationConfig.productName()),
                """
                        Returns system information about the %s installation, including
                        cluster ID, installed version, hostname, timezone, and operating system.
                        """.formatted(customizationConfig.productName()),
                objectMapper,
                clusterConfigService,
                schemaGeneratorProvider
        );
        this.serverStatus = serverStatus;
        this.leaderElectionService = leaderElectionService;
        this.customizationConfig = customizationConfig;
    }

    @Override
    public SystemOverviewResponse apply(PermissionHelper permissionHelper, SystemInfoTool.Parameters unused) {
        try {
            // TODO: find a better way to do this. This is all verbatim from org.graylog2.shared.rest.resources.system.SystemResource::system

            permissionHelper.checkPermission(RestPermissions.SYSTEM_READ, serverStatus.getNodeId().toString());

            return SystemOverviewResponse.create(f("%s Server", customizationConfig.productName()),
                    ServerVersion.CODENAME,
                    serverStatus.getNodeId().toString(),
                    serverStatus.getClusterId(),
                    ServerVersion.VERSION.toString(),
                    Tools.getISO8601String(serverStatus.getStartedAt()),
                    serverStatus.isProcessing(),
                    Tools.getLocalCanonicalHostname(),
                    serverStatus.getLifecycle().getDescription().toLowerCase(Locale.ENGLISH),
                    serverStatus.getLifecycle().getLoadbalancerStatus().toString().toLowerCase(Locale.ENGLISH),
                    serverStatus.getTimezone().getID(),
                    System.getProperty("os.name", "unknown") + " " + System.getProperty("os.version", "unknown"),
                    leaderElectionService.isLeader()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class Parameters {}
}
