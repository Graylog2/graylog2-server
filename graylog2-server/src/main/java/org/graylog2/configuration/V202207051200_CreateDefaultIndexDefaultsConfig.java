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

import org.graylog2.indexer.management.IndexManagementConfig;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.ZonedDateTime;

import static com.google.common.base.Preconditions.checkState;
import static org.graylog2.configuration.IndexSetsDefaultsConfiguration.DEFAULT_FIELD_TYPE_REFRESH_INTERVAL;
import static org.graylog2.configuration.IndexSetsDefaultsConfiguration.DEFAULT_FIELD_TYPE_REFRESH_INTERVAL_UNIT;

public class V202207051200_CreateDefaultIndexDefaultsConfig extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V202207051200_CreateDefaultIndexDefaultsConfig.class);

    private final ClusterConfigService clusterConfigService;
    private final ElasticsearchConfiguration elasticsearchConfig;

    @Inject
    public V202207051200_CreateDefaultIndexDefaultsConfig(final ClusterConfigService clusterConfigService,
                                                          final ElasticsearchConfiguration elasticsearchConfig) {
        this.clusterConfigService = clusterConfigService;
        this.elasticsearchConfig = elasticsearchConfig;
    }

    @Override
    public ZonedDateTime createdAt() {
        // this migration should run early
        return ZonedDateTime.parse("2022-08-05T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(IndexSetsDefaultsConfiguration.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final IndexManagementConfig legacyConfig = clusterConfigService.get(IndexManagementConfig.class);

        try {
            final IndexSetsDefaultsConfiguration config = IndexSetsDefaultsConfiguration.builder()
                    .indexPrefix(elasticsearchConfig.getIndexPrefix())
                    .indexAnalyzer(elasticsearchConfig.getAnalyzer())
                    .shards(elasticsearchConfig.getShards())
                    .replicas(elasticsearchConfig.getReplicas())
                    .indexOptimizationDisabled(elasticsearchConfig.isDisableIndexOptimization())
                    .indexOptimizationMaxNumSegments(elasticsearchConfig.getIndexOptimizationMaxNumSegments())
                    .fieldTypeRefreshInterval(DEFAULT_FIELD_TYPE_REFRESH_INTERVAL)
                    .fieldTypeRefreshIntervalUnit(DEFAULT_FIELD_TYPE_REFRESH_INTERVAL_UNIT)
//                    .rotationStrategyClass(DEFAULT_ROTATION_STRATEGY_CLASS)
//                    .rotationStrategyConfig(DEFAULT_ROTATION_STRATEGY_CONFIG)
//                    .retentionStrategyClass(DEFAULT_RETENTION_STRATEGY_CLASS)
//                    .retentionStrategyConfig(DEFAULT_RETENTION_STRATEGY_CONFIG)
                    .build();
            clusterConfigService.write(config);
            LOG.debug("Index defaults config saved.");
        } catch (Exception e) {
            LOG.error("Unable to write index defaults configuration.", e);
        }
    }
}
