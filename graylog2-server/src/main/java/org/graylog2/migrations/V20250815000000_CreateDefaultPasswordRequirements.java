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
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.users.PasswordComplexityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

public class V20250815000000_CreateDefaultPasswordRequirements extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20250815000000_CreateDefaultPasswordRequirements.class);

    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20250815000000_CreateDefaultPasswordRequirements(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-08-15T00:00:00Z");
    }

    @Override
    public void upgrade() {
        final PasswordComplexityConfig config = clusterConfigService.get(PasswordComplexityConfig.class);
        if (config != null) {
            LOG.debug("Found password complexity configuration, no migration necessary: {}", config);
            return;
        }

        try {
            final PasswordComplexityConfig defaultConfig = PasswordComplexityConfig.DEFAULT;
            clusterConfigService.write(defaultConfig);
            LOG.debug("Created default password complexity configuration: {}", defaultConfig);
        } catch (Exception e) {
            LOG.error("Unable to write default password complexity configuration", e);
        }
    }
}
