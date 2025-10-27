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
package org.graylog2.migrations;

import jakarta.inject.Inject;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

/**
 * Add storage for MCP configuration settings.
 */
public class V20251003000000_AddMcpConfigurationMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20251003000000_AddMcpConfigurationMigration.class);

    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20251003000000_AddMcpConfigurationMigration(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-10-03T00:00:00Z");
    }

    @Override
    public void upgrade() {
        final McpConfiguration mcpConfig = clusterConfigService.get(McpConfiguration.class);
        if (mcpConfig == null) {
            final McpConfiguration config = McpConfiguration.DEFAULT_VALUES;
            LOG.info("Creating MCP config: {}", config);
            clusterConfigService.write(config);
        }
    }
}
