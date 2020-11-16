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
package org.graylog.plugins.sidecar.migrations;

import org.graylog.plugins.sidecar.system.SidecarConfiguration;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V20180601151500_AddDefaultConfiguration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20180601151500_AddDefaultConfiguration.class);

     private final ClusterConfigService clusterConfigService;

    @Inject
    public V20180601151500_AddDefaultConfiguration(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2018-06-01T15:15:00Z");
    }

    @Override
    public void upgrade() {
        final SidecarConfiguration sidecarConfiguration = clusterConfigService.get(SidecarConfiguration.class);
        if (sidecarConfiguration == null) {
            final SidecarConfiguration config = SidecarConfiguration.defaultConfiguration();
            LOG.info("Creating Sidecar cluster config: {}", config);
            clusterConfigService.write(config);
        }
    }
}
