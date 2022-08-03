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
package org.graylog2.configuration;

import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V202207061200_CreateDefaultIndexDefaultsConfig extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V202207061200_CreateDefaultIndexDefaultsConfig.class);

    private final ClusterConfigService clusterConfigService;

    @Inject
    public V202207061200_CreateDefaultIndexDefaultsConfig(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        // this migration should run early
        return ZonedDateTime.parse("2022-07-06T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(IndexSetsDefaultsConfiguration.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }
        try {
            clusterConfigService.write(IndexSetsDefaultsConfiguration.createNew());
            LOG.debug("Index defaults config saved.");
        } catch (Exception e) {
            LOG.error("Unable to write index defaults configuration.", e);
        }
    }
}
