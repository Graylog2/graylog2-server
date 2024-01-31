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
import jakarta.inject.Provider;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.datatiering.hotonly.HotOnlyDataTieringConfig;
import org.graylog2.indexer.retention.strategies.ClosingRetentionStrategy;
import org.graylog2.indexer.retention.strategies.ClosingRetentionStrategyConfig;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategy;
import org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategy;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategyConfig;
import org.graylog2.indexer.rotation.tso.IndexLifetimeConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MaintenanceStrategiesHelper {
    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceStrategiesHelper.class);
    private final Map<String, Provider<RotationStrategy>> rotationStrategies;
    private final Map<String, Provider<RetentionStrategy>> retentionStrategies;
    private final ClusterConfigService clusterConfigService;
    private final ElasticsearchConfiguration elasticsearchConfiguration;


    @Inject
    public MaintenanceStrategiesHelper(Map<String, Provider<RotationStrategy>> rotationStrategies, Map<String, Provider<RetentionStrategy>> retentionStrategies, ClusterConfigService clusterConfigService, ElasticsearchConfiguration elasticsearchConfiguration) {
        this.rotationStrategies = rotationStrategies;
        this.retentionStrategies = retentionStrategies;
        this.clusterConfigService = clusterConfigService;
        this.elasticsearchConfiguration = elasticsearchConfiguration;
    }

    public ImmutablePair<String, RotationStrategyConfig> readRotationConfigFromServerConf() {
        switch (elasticsearchConfiguration.getRotationStrategy()) {
            case SizeBasedRotationStrategy.NAME -> {
                return ImmutablePair.of(SizeBasedRotationStrategy.class.getCanonicalName(),
                        SizeBasedRotationStrategyConfig.create(elasticsearchConfiguration.getMaxSizePerIndex()));
            }
            case TimeBasedRotationStrategy.NAME -> {
                return ImmutablePair.of(TimeBasedRotationStrategy.class.getCanonicalName(),
                        TimeBasedRotationStrategyConfig.builder()
                                .rotationPeriod(elasticsearchConfiguration.getMaxTimePerIndex())
                                .maxRotationPeriod(elasticsearchConfiguration.getMaxWriteIndexAge())
                                .rotateEmptyIndexSet(elasticsearchConfiguration.isRotateEmptyIndex())
                                .build());
            }
            case MessageCountRotationStrategy.NAME -> {
                return ImmutablePair.of(MessageCountRotationStrategy.class.getCanonicalName(),
                        MessageCountRotationStrategyConfig.create(elasticsearchConfiguration.getMaxDocsPerIndex()));
            }
            case TimeBasedSizeOptimizingStrategy.NAME -> {
                return ImmutablePair.of(TimeBasedSizeOptimizingStrategy.class.getCanonicalName(),
                        TimeBasedSizeOptimizingStrategyConfig.builder()
                                .indexLifetimeMin(elasticsearchConfiguration.getTimeSizeOptimizingRetentionMinLifeTime())
                                .indexLifetimeMax(elasticsearchConfiguration.getTimeSizeOptimizingRetentionMaxLifeTime())
                                .build());
            }
            default -> {
                LOG.warn("Unknown retention strategy [{}]. Defaulting to [{}]",
                        elasticsearchConfiguration.getRotationStrategy(), MessageCountRotationStrategy.NAME);
                return ImmutablePair.of(MessageCountRotationStrategy.class.getCanonicalName(),
                        MessageCountRotationStrategyConfig.create(elasticsearchConfiguration.getMaxDocsPerIndex()));
            }
        }
    }

    public ImmutablePair<String, RetentionStrategyConfig> readRetentionConfigFromServerConf() {
        switch (elasticsearchConfiguration.getRetentionStrategy()) {
            case ClosingRetentionStrategy.NAME -> {
                return ImmutablePair.of(ClosingRetentionStrategy.class.getCanonicalName(),
                        ClosingRetentionStrategyConfig.create(elasticsearchConfiguration.getMaxNumberOfIndices()));
            }
            case DeletionRetentionStrategy.NAME -> {
                return ImmutablePair.of(DeletionRetentionStrategy.class.getCanonicalName(),
                        DeletionRetentionStrategyConfig.create(elasticsearchConfiguration.getMaxNumberOfIndices()));
            }
            default -> {
                LOG.warn("Unknown retention strategy [{}]. Defaulting to [{}].", elasticsearchConfiguration.getRetentionStrategy(),
                        DeletionRetentionStrategy.NAME);
                return ImmutablePair.of(DeletionRetentionStrategy.class.getCanonicalName(),
                        DeletionRetentionStrategyConfig.create(elasticsearchConfiguration.getMaxNumberOfIndices()));
            }
        }
    }

    public HotOnlyDataTieringConfig defaultDataTieringConfig() {
        return HotOnlyDataTieringConfig.builder()
                .indexLifetimeMin(IndexLifetimeConfig.DEFAULT_LIFETIME_MIN)
                .indexLifetimeMax(IndexLifetimeConfig.DEFAULT_LIFETIME_MAX)
                .build();
    }
}
