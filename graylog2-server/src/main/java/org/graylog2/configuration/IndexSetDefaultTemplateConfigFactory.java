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

import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.datatiering.hotonly.HotOnlyDataTieringConfig;
import org.graylog2.indexer.indexset.SimpleIndexSetConfig;
import org.graylog2.indexer.indexset.template.IndexSetTemplateConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategyConfig;
import org.graylog2.migrations.MaintenanceStrategiesHelper;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;

public class IndexSetDefaultTemplateConfigFactory {

    final private ElasticsearchConfiguration elasticsearchConfiguration;
    final private MaintenanceStrategiesHelper maintenanceStrategiesHelper;

    @Inject
    public IndexSetDefaultTemplateConfigFactory(ElasticsearchConfiguration elasticsearchConfiguration, MaintenanceStrategiesHelper maintenanceStrategiesHelper) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.maintenanceStrategiesHelper = maintenanceStrategiesHelper;
    }

    public IndexSetTemplateConfig create() {
        final ImmutablePair<String, RotationStrategyConfig> rotationConfig =
                maintenanceStrategiesHelper.readRotationConfigFromServerConf();
        final ImmutablePair<String, RetentionStrategyConfig> retentionConfig =
                maintenanceStrategiesHelper.readRetentionConfigFromServerConf();
        return IndexSetTemplateConfig.builder()
                .indexAnalyzer(elasticsearchConfiguration.getAnalyzer())
                .shards(elasticsearchConfiguration.getShards())
                .replicas(elasticsearchConfiguration.getReplicas())
                .indexOptimizationDisabled(elasticsearchConfiguration.isDisableIndexOptimization())
                .indexOptimizationMaxNumSegments(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments())
                .fieldTypeRefreshInterval(SimpleIndexSetConfig.DEFAULT_FIELD_TYPE_REFRESH_INTERVAL)
                .rotationStrategyClass(rotationConfig.left)
                .rotationStrategy(rotationConfig.right)
                .retentionStrategyClass(retentionConfig.left)
                .retentionStrategy(retentionConfig.right)
                .useLegacyRotation(false)
                .dataTiering(maintenanceStrategiesHelper.defaultDataTieringConfig())
                .build();
    }

    public DataTieringConfig getDataTieringConfig(RotationStrategyConfig rotationStrategyConfig) {
        if (rotationStrategyConfig instanceof TimeBasedSizeOptimizingStrategyConfig config) {
            // Take already configured min/max settings from TSO
            return HotOnlyDataTieringConfig.builder()
                    .indexLifetimeMin(config.indexLifetimeMin())
                    .indexLifetimeMax(config.indexLifetimeMax())
                    .build();
        }
        return maintenanceStrategiesHelper.defaultDataTieringConfig();
    }
}
