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

import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.plugin.cluster.ClusterIdFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V19700101000000_GenerateClusterId extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V19700101000000_GenerateClusterId.class);

    private final ClusterConfigService clusterConfigService;
    private final ClusterIdFactory clusterIdFactory;

    @Inject
    public V19700101000000_GenerateClusterId(ClusterConfigService clusterConfigService, ClusterIdFactory clusterIdFactory) {
        this.clusterConfigService = clusterConfigService;
        this.clusterIdFactory = clusterIdFactory;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("1970-01-01T00:00:00Z");
    }

    @Override
    public void upgrade() {
        if(clusterConfigService.get(ClusterId.class) == null) {
            ClusterId clusterId = clusterIdFactory.create();
            clusterConfigService.write(clusterId);

            LOG.debug("Generated cluster ID {}", clusterId.clusterId());
        }
    }
}
