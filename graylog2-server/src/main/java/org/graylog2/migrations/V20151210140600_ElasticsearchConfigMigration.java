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
import org.graylog2.indexer.management.IndexManagementConfig;
import org.graylog2.indexer.retention.strategies.ClosingRetentionStrategyConfig;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategyConfig;
import org.graylog2.indexer.searches.SearchesClusterConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V20151210140600_ElasticsearchConfigMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20151210140600_ElasticsearchConfigMigration.class);

    private final ClusterConfigService clusterConfigService;
    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final MaintenanceStrategiesHelper maintenanceStrategiesHelper;

    @Inject
    public V20151210140600_ElasticsearchConfigMigration(ClusterConfigService clusterConfigService, ElasticsearchConfiguration elasticsearchConfiguration, MaintenanceStrategiesHelper maintenanceStrategiesHelper) {
        this.clusterConfigService = clusterConfigService;
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.maintenanceStrategiesHelper = maintenanceStrategiesHelper;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2015-12-10T14:06:00Z");
    }

    // Migrate old Elasticsearch config settings to new ClusterConfig based ones.
    @Override
    public void upgrade() {
        // All default rotation strategy settings.
        final MessageCountRotationStrategyConfig messageCountRotationStrategyConfig = clusterConfigService.get(MessageCountRotationStrategyConfig.class);
        final SizeBasedRotationStrategyConfig sizeBasedRotationStrategyConfig = clusterConfigService.get(SizeBasedRotationStrategyConfig.class);
        final TimeBasedRotationStrategyConfig timeBasedRotationStrategyConfig = clusterConfigService.get(TimeBasedRotationStrategyConfig.class);
        final TimeBasedSizeOptimizingStrategyConfig timeBasedSizeOptimizingStrategyConfig = clusterConfigService.get(TimeBasedSizeOptimizingStrategyConfig.class);

        if (messageCountRotationStrategyConfig == null) {
            final MessageCountRotationStrategyConfig countConfig = MessageCountRotationStrategyConfig.create(elasticsearchConfiguration.getMaxDocsPerIndex());
            clusterConfigService.write(countConfig);
            LOG.info("Migrated \"{}\" setting: {}", "elasticsearch_max_docs_per_index", countConfig);
        }
        if (sizeBasedRotationStrategyConfig == null) {
            final SizeBasedRotationStrategyConfig sizeConfig = SizeBasedRotationStrategyConfig.create(elasticsearchConfiguration.getMaxSizePerIndex());
            clusterConfigService.write(sizeConfig);
            LOG.info("Migrated \"{}\" setting: {}", "elasticsearch_max_size_per_index", sizeConfig);
        }
        if (timeBasedRotationStrategyConfig == null) {
            final TimeBasedRotationStrategyConfig timeConfig =
                    TimeBasedRotationStrategyConfig.builder()
                            .rotationPeriod(elasticsearchConfiguration.getMaxTimePerIndex())
                            .maxRotationPeriod(elasticsearchConfiguration.getMaxWriteIndexAge())
                            .rotateEmptyIndexSet(elasticsearchConfiguration.isRotateEmptyIndex())
                            .build();
            clusterConfigService.write(timeConfig);
            LOG.info("Migrated \"{}\" setting: {}", "elasticsearch_max_time_per_index", timeConfig);
        }
        if (timeBasedSizeOptimizingStrategyConfig == null) {
            // This migration stores the current server.conf rotation/retention configuration state in the
            // CusterConfig so that a later migration can pick up the settings and migrate them to the newer
            // IndexSetConfig object (that backs the Index Sets page). Any default-specified rotation/retention
            // strategy must have a corresponding config entity persisted in the cluster config in order for
            // the org.graylog2.migrations.V20161116172100_DefaultIndexSetMigration migration to use it when
            // creating the Default index set. Since TimeBasedSizeOptimizingStrategy is the default (at the time
            // of writing this code), then it must also appear here and be stored in the cluster config for the
            // Default index set to use it as the default rotation strategy.
            final TimeBasedSizeOptimizingStrategyConfig timeSizeConfig =
                    maintenanceStrategiesHelper.buildDefaultTimeSizeStrategy();
            clusterConfigService.write(timeSizeConfig);
            LOG.info("Stored legacy rotation config setting \"{}\" setting.", timeSizeConfig);
        }

        // All default retention strategy settings
        final ClosingRetentionStrategyConfig closingRetentionStrategyConfig = clusterConfigService.get(ClosingRetentionStrategyConfig.class);
        final DeletionRetentionStrategyConfig deletionRetentionStrategyConfig = clusterConfigService.get(DeletionRetentionStrategyConfig.class);

        if (closingRetentionStrategyConfig == null) {
            final ClosingRetentionStrategyConfig closingConfig = ClosingRetentionStrategyConfig.create(elasticsearchConfiguration.getMaxNumberOfIndices());
            clusterConfigService.write(closingConfig);
            LOG.info("Migrated \"{}\" setting: {}", "elasticsearch_max_number_of_indices", closingConfig);
        }

        if (deletionRetentionStrategyConfig == null) {
            final DeletionRetentionStrategyConfig deletionConfig = DeletionRetentionStrategyConfig.create(elasticsearchConfiguration.getMaxNumberOfIndices());
            clusterConfigService.write(deletionConfig);
            LOG.info("Migrated \"{}\" setting: {}", "elasticsearch_max_number_of_indices", deletionConfig);
        }

        // Selected rotation and retention strategies.
        final IndexManagementConfig indexManagementConfig = clusterConfigService.get(IndexManagementConfig.class);
        if (indexManagementConfig == null) {
            // If previous cluster config IndexManagementConfig entity was not defined, then rely on defaults
            // specified in the server.conf file. This is the default scenario for new Graylog server instances.
            final IndexManagementConfig config = IndexManagementConfig.create(
                    maintenanceStrategiesHelper.readRotationConfigFromServerConf().left,
                    maintenanceStrategiesHelper.readRetentionConfigFromServerConf().left);
            clusterConfigService.write(config);
            LOG.info("Migrated \"{}\" and \"{}\" setting: {}", "rotation_strategy", "retention_strategy", config);
        }

        final SearchesClusterConfig searchesClusterConfig = clusterConfigService.get(SearchesClusterConfig.class);
        if (searchesClusterConfig == null) {
            final SearchesClusterConfig config = SearchesClusterConfig.createDefault();
            LOG.info("Creating searches cluster config: {}", config);
            clusterConfigService.write(config);
        }
    }
}
