/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.periodical;

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.management.IndexManagementConfig;
import org.graylog2.indexer.rotation.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.MessageCountRotationStrategyConfig;
import org.graylog2.indexer.rotation.SizeBasedRotationStrategy;
import org.graylog2.indexer.rotation.SizeBasedRotationStrategyConfig;
import org.graylog2.indexer.rotation.TimeBasedRotationStrategy;
import org.graylog2.indexer.rotation.TimeBasedRotationStrategyConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class ConfigurationManagementPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManagementPeriodical.class);
    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public ConfigurationManagementPeriodical(ElasticsearchConfiguration elasticsearchConfiguration,
                                             ClusterConfigService clusterConfigService) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.clusterConfigService = clusterConfigService;
    }

    // Migrate old Elasticsearch config settings to new ClusterConfig based ones.
    public void migrateElasticsearchConfig() {
        // All default rotation strategy settings.
        final MessageCountRotationStrategyConfig messageCountRotationStrategyConfig = clusterConfigService.get(MessageCountRotationStrategyConfig.class);
        final SizeBasedRotationStrategyConfig sizeBasedRotationStrategyConfig = clusterConfigService.get(SizeBasedRotationStrategyConfig.class);
        final TimeBasedRotationStrategyConfig timeBasedRotationStrategyConfig = clusterConfigService.get(TimeBasedRotationStrategyConfig.class);

        if (messageCountRotationStrategyConfig == null) {
            final MessageCountRotationStrategyConfig config = MessageCountRotationStrategyConfig.create(elasticsearchConfiguration.getMaxDocsPerIndex());
            clusterConfigService.write(config);
            LOG.info("Migrated \"{}\" setting: {}", "elasticsearch_max_docs_per_index", config);
        }
        if (sizeBasedRotationStrategyConfig == null) {
            final SizeBasedRotationStrategyConfig config = SizeBasedRotationStrategyConfig.create(elasticsearchConfiguration.getMaxSizePerIndex());
            clusterConfigService.write(config);
            LOG.info("Migrated \"{}\" setting: {}", "elasticsearch_max_size_per_index", config);
        }
        if (timeBasedRotationStrategyConfig == null) {
            final TimeBasedRotationStrategyConfig config = TimeBasedRotationStrategyConfig.create(elasticsearchConfiguration.getMaxTimePerIndex());
            clusterConfigService.write(config);
            LOG.info("Migrated \"{}\" setting: {}", "elasticsearch_max_time_per_index", config);
        }

        // Selected rotation strategy.
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
                default:
                    rotationStrategyClass = MessageCountRotationStrategy.class;
            }

            final IndexManagementConfig config = IndexManagementConfig.create(rotationStrategyClass.getCanonicalName());
            clusterConfigService.write(config);
            LOG.info("Migrated \"{}\" setting: {}", "rotation_strategy", config);
        }
    }

    @Override
    public void doRun() {
        migrateElasticsearchConfig();
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean masterOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 0;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
