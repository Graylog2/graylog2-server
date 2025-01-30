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
import jakarta.inject.Named;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.buffers.processors.TimeStampConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * Migration to set timestamp handling default values based on the Graylog version.
 */
public class V202501301750_DefaultTimeStampConfig extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V202501301750_DefaultTimeStampConfig.class);

    private final boolean isFreshInstallation;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V202501301750_DefaultTimeStampConfig(@Named("isFreshInstallation") boolean isFreshInstallation,
                                                final ClusterConfigService clusterConfigService) {
        this.isFreshInstallation = isFreshInstallation;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-01-30T17:50:00Z");
    }

    @Override
    public void upgrade() {
        // Do not run for existing installations.
        // TODO: consider making this default for 7.0 and beyond.
        if (!isFreshInstallation) {
            return;
        }
        // Do not run again if the configuration can already be found in the database.
        if (clusterConfigService.get(TimeStampConfig.class) != null) {
            return;
        }

        clusterConfigService.write(new TimeStampConfig(Duration.ofDays(2)));

        LOG.debug("Successfully set timestamp handling default values.");
    }

}
