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
import org.graylog2.indexer.searches.SearchesClusterConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V20151210140600_ElasticsearchConfigMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20151210140600_ElasticsearchConfigMigration.class);

    private final ClusterConfigService clusterConfigService;
    private final ElasticsearchConfiguration elasticsearchConfiguration;

    @Inject
    public V20151210140600_ElasticsearchConfigMigration(ClusterConfigService clusterConfigService, ElasticsearchConfiguration elasticsearchConfiguration) {
        this.clusterConfigService = clusterConfigService;
        this.elasticsearchConfiguration = elasticsearchConfiguration;
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
            final TimeBasedRotationStrategyConfig timeConfig = TimeBasedRotationStrategyConfig.create(elasticsearchConfiguration.getMaxTimePerIndex());
            clusterConfigService.write(timeConfig);
            LOG.info("Migrated \"{}\" setting: {}", "elasticsearch_max_time_per_index", timeConfig);
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
            final Class<? extends RotationStrategy> rotationStrategyClass;
            switch (elasticsearchConfiguration.getRotationStrategy()) {
                case "size":
                    rotationStrategyClass = SizeBasedRotationStrategy.class;
                    break;
                case "time":
                    rotationStrategyClass = TimeBasedRotationStrategy.class;
                    break;
                case "count":
                    rotationStrategyClass = MessageCountRotationStrategy.class;
                    break;
                default:
                    LOG.warn("Unknown retention strategy \"{}\"", elasticsearchConfiguration.getRotationStrategy());
                    rotationStrategyClass = MessageCountRotationStrategy.class;
            }

            final Class<? extends RetentionStrategy> retentionStrategyClass;
            switch (elasticsearchConfiguration.getRetentionStrategy()) {
                case "close":
                    retentionStrategyClass = ClosingRetentionStrategy.class;
                    break;
                case "delete":
                    retentionStrategyClass = DeletionRetentionStrategy.class;
                    break;
                default:
                    LOG.warn("Unknown retention strategy \"{}\"", elasticsearchConfiguration.getRetentionStrategy());
                    retentionStrategyClass = DeletionRetentionStrategy.class;
            }

            final IndexManagementConfig config = IndexManagementConfig.create(
                    rotationStrategyClass.getCanonicalName(),
                    retentionStrategyClass.getCanonicalName());
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
