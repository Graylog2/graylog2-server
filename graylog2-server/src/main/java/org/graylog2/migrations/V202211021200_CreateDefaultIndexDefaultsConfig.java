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

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.configuration.IndexSetsDefaultConfiguration;
import org.graylog2.configuration.IndexSetsDefaultConfigurationFactory;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;

/**
 * Create initial index set default configuration based on {@link ElasticsearchConfiguration} values.
 */
public class V202211021200_CreateDefaultIndexDefaultsConfig extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V202211021200_CreateDefaultIndexDefaultsConfig.class);

    private final ClusterConfigService clusterConfigService;
    private final IndexSetsDefaultConfigurationFactory factory;

    @Inject
    public V202211021200_CreateDefaultIndexDefaultsConfig(final ClusterConfigService clusterConfigService,
                                                          IndexSetsDefaultConfigurationFactory factory) {
        this.clusterConfigService = clusterConfigService;
        this.factory = factory;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2022-11-02T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(IndexSetsDefaultConfiguration.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        try {
            clusterConfigService.write(factory.create());
            LOG.debug("IndexSetsDefaultConfiguration saved.");
        } catch (Exception e) {
            LOG.error("Unable to write IndexSetsDefaultConfiguration.", e);
        }
    }
}
