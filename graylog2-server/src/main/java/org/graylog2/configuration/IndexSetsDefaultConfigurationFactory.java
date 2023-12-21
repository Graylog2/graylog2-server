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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.migrations.MaintenanceStrategiesHelper;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;

import jakarta.inject.Inject;

import java.util.concurrent.TimeUnit;

public class IndexSetsDefaultConfigurationFactory {

    final private ElasticsearchConfiguration elasticsearchConfiguration;
    final private MaintenanceStrategiesHelper maintenanceStrategiesHelper;

    @Inject
    public IndexSetsDefaultConfigurationFactory(ElasticsearchConfiguration elasticsearchConfiguration, MaintenanceStrategiesHelper maintenanceStrategiesHelper) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.maintenanceStrategiesHelper = maintenanceStrategiesHelper;
    }

    public IndexSetsDefaultConfiguration create() {
        final ImmutablePair<String, RotationStrategyConfig> rotationConfig =
                maintenanceStrategiesHelper.readRotationConfigFromServerConf();
        final ImmutablePair<String, RetentionStrategyConfig> retentionConfig =
                maintenanceStrategiesHelper.readRetentionConfigFromServerConf();
        return IndexSetsDefaultConfiguration.builder()
                .indexAnalyzer(elasticsearchConfiguration.getAnalyzer())
                .shards(elasticsearchConfiguration.getShards())
                .replicas(elasticsearchConfiguration.getReplicas())
                .indexOptimizationDisabled(elasticsearchConfiguration.isDisableIndexOptimization())
                .indexOptimizationMaxNumSegments(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments())
                .fieldTypeRefreshInterval(IndexSetConfig.DEFAULT_FIELD_TYPE_REFRESH_INTERVAL.getStandardSeconds())
                .fieldTypeRefreshIntervalUnit(TimeUnit.SECONDS)
                .rotationStrategyClass(rotationConfig.left)
                .rotationStrategyConfig(rotationConfig.right)
                .retentionStrategyClass(retentionConfig.left)
                .retentionStrategyConfig(retentionConfig.right).build();
    }
}
