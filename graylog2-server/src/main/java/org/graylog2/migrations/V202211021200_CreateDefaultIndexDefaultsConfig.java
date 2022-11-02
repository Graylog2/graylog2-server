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
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Create initial index set default configuration based on {@link ElasticsearchConfiguration} values.
 */
public class V202211021200_CreateDefaultIndexDefaultsConfig extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V202211021200_CreateDefaultIndexDefaultsConfig.class);

    private final ClusterConfigService clusterConfigService;
    private final ElasticsearchConfiguration config;
    private final Map<String, Provider<RotationStrategy>> rotationStrategies;
    private final Map<String, Provider<RetentionStrategy>> retentionStrategies;

    @Inject
    public V202211021200_CreateDefaultIndexDefaultsConfig(final ClusterConfigService clusterConfigService,
                                                          final ElasticsearchConfiguration config, Map<String, Provider<RotationStrategy>> rotationStrategies, Map<String, Provider<RetentionStrategy>> retentionStrategies) {
        this.clusterConfigService = clusterConfigService;
        this.config = config;
        this.rotationStrategies = rotationStrategies;
        this.retentionStrategies = retentionStrategies;
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
            final IndexSetsDefaultConfiguration.Builder builder = IndexSetsDefaultConfiguration.builder()
                    .indexAnalyzer(config.getAnalyzer())
                    .shards(config.getShards())
                    .replicas(config.getReplicas())
                    .indexOptimizationDisabled(config.isDisableIndexOptimization())
                    .indexOptimizationMaxNumSegments(config.getIndexOptimizationMaxNumSegments())
                    .fieldTypeRefreshInterval(config.getIndexFieldTypePeriodicalFullRefreshInterval().toSeconds())
                    .fieldTypeRefreshIntervalUnit(TimeUnit.SECONDS);

            // Rotation strategy
            switch (config.getRotationStrategy()) {
                case SizeBasedRotationStrategy.NAME -> {
                    builder.rotationStrategyClass(SizeBasedRotationStrategy.class.getCanonicalName())
                            .rotationStrategyConfig(SizeBasedRotationStrategyConfig.create(config.getMaxSizePerIndex()));
                }
                case TimeBasedRotationStrategy.NAME -> {
                    builder.rotationStrategyClass(TimeBasedRotationStrategy.class.getCanonicalName())
                            .rotationStrategyConfig(TimeBasedRotationStrategyConfig.builder()
                                    .rotationPeriod(config.getMaxTimePerIndex())
                                    .maxRotationPeriod(config.getMaxWriteIndexAge())
                                    .rotateEmptyIndexSet(config.isRotateEmptyIndex())
                                    .build());
                }
                case MessageCountRotationStrategy.NAME -> {
                    builder.rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
                            .rotationStrategyConfig(MessageCountRotationStrategyConfig.create(config.getMaxDocsPerIndex()));
                }
                default -> {
                    LOG.warn("Unknown retention strategy [{}]. Defaulting to [{}]",
                            config.getRotationStrategy(), MessageCountRotationStrategy.NAME);
                    builder.rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
                            .rotationStrategyConfig(MessageCountRotationStrategyConfig.create(config.getMaxDocsPerIndex()));
                }
            }

            // Retention strategy
            switch (config.getRetentionStrategy()) {
                case ClosingRetentionStrategy.NAME -> {
                    builder.retentionStrategyClass(ClosingRetentionStrategy.class.getCanonicalName())
                            .retentionStrategyConfig(ClosingRetentionStrategyConfig.create(config.getMaxNumberOfIndices()));
                }
                case DeletionRetentionStrategy.NAME -> {
                    builder.retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
                            .retentionStrategyConfig(DeletionRetentionStrategyConfig.create(config.getMaxNumberOfIndices()));
                }
                // TODO: Handle archive? Must be in Enterprise.
                default -> {
                    LOG.warn("Unknown retention strategy [{}]. Defaulting to [{}].", config.getRetentionStrategy());
                    builder.retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
                            .retentionStrategyConfig(DeletionRetentionStrategyConfig.create(config.getMaxNumberOfIndices()));
                }
            }

            clusterConfigService.write(builder.build());
            LOG.debug("Index defaults config saved.");
        } catch (Exception e) {
            LOG.error("Unable to write index defaults configuration.", e);
        }
    }
}
