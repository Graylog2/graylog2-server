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
package org.graylog2.indexer.indexset;

import org.graylog2.configuration.IndexSetsDefaultConfiguration;
import org.graylog2.configuration.IndexSetsDefaultConfigurationFactory;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class IndexSetConfigFactory {
    private static final Logger LOG = LoggerFactory.getLogger(IndexSetConfigFactory.class);
    private final IndexSetsDefaultConfigurationFactory indexSetsDefaultConfigurationFactory;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public IndexSetConfigFactory(IndexSetsDefaultConfigurationFactory indexSetsDefaultConfigurationFactory,
                                 ClusterConfigService clusterConfigService) {
        this.indexSetsDefaultConfigurationFactory = indexSetsDefaultConfigurationFactory;
        this.clusterConfigService = clusterConfigService;
    }

    public IndexSetConfig.Builder createDefault() {
        IndexSetsDefaultConfiguration defaultConfig = clusterConfigService.get(IndexSetsDefaultConfiguration.class);
        if (defaultConfig == null) {
            // Valid case for migrations that existed before in-DB index configuration was established in Graylog 5.1
            LOG.debug("Could not find IndexSetsDefaultConfiguration. Falling back to server configuration values.");
            defaultConfig = indexSetsDefaultConfigurationFactory.create();
        }

        return IndexSetConfig.builder()
                .creationDate(getCreationDate())
                .indexAnalyzer(defaultConfig.indexAnalyzer())
                .shards(defaultConfig.shards())
                .replicas(defaultConfig.replicas())
                .indexOptimizationDisabled(defaultConfig.indexOptimizationDisabled())
                .indexOptimizationMaxNumSegments(defaultConfig.indexOptimizationMaxNumSegments())
                .fieldTypeRefreshInterval(Duration.standardSeconds(
                        defaultConfig.fieldTypeRefreshIntervalUnit().toSeconds(defaultConfig.fieldTypeRefreshInterval())))
                .rotationStrategyClass(defaultConfig.rotationStrategyClass())
                .rotationStrategy(defaultConfig.rotationStrategyConfig())
                .retentionStrategyClass(defaultConfig.retentionStrategyClass())
                .retentionStrategy(defaultConfig.retentionStrategyConfig());
    }

    private static ZonedDateTime getCreationDate() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }
}
