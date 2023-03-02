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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.configuration.IndexSetsDefaultConfiguration;
import org.graylog2.migrations.MaintenanceStrategiesHelper;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class IndexSetConfigFactory {
    private static final Logger LOG = LoggerFactory.getLogger(IndexSetConfigFactory.class);
    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final MaintenanceStrategiesHelper maintenanceStrategiesHelper;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public IndexSetConfigFactory(ElasticsearchConfiguration elasticsearchConfiguration,
                                 MaintenanceStrategiesHelper maintenanceStrategiesHelper, ClusterConfigService clusterConfigService) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.maintenanceStrategiesHelper = maintenanceStrategiesHelper;
        this.clusterConfigService = clusterConfigService;
    }

    public IndexSetConfig.Builder createDefault() {
        final IndexSetsDefaultConfiguration defaultConfig = clusterConfigService.get(IndexSetsDefaultConfiguration.class);
        if (defaultConfig == null) {
            // Valid case for migrations that existed before in-DB index configuration was established in Graylog 5.1
            LOG.debug("Could not find IndexSetsDefaultConfiguration. Falling back to server configuration values.");
            return createInitialFromServerConfig();
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

    private IndexSetConfig.Builder createInitialFromServerConfig() {
        final ImmutablePair<String, RotationStrategyConfig> rotationConfig =
                maintenanceStrategiesHelper.readRotationConfigFromServerConf();
        final ImmutablePair<String, RetentionStrategyConfig> retentionConfig =
                maintenanceStrategiesHelper.readRetentionConfigFromServerConf();
        return IndexSetConfig.builder()
                .creationDate(getCreationDate())
                .shards(elasticsearchConfiguration.getShards())
                .replicas(elasticsearchConfiguration.getReplicas())
                .rotationStrategyClass(rotationConfig.left)
                .rotationStrategy(rotationConfig.right)
                .retentionStrategyClass(retentionConfig.left)
                .retentionStrategy(retentionConfig.right)
                .indexAnalyzer(elasticsearchConfiguration.getAnalyzer())
                .indexOptimizationMaxNumSegments(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(elasticsearchConfiguration.isDisableIndexOptimization())
                .fieldTypeRefreshInterval(IndexSetConfig.DEFAULT_FIELD_TYPE_REFRESH_INTERVAL);
    }

    private static ZonedDateTime getCreationDate() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }

}
