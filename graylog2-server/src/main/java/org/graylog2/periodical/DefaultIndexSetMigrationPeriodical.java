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
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.DefaultIndexSetCreated;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.management.IndexManagementConfig;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.streams.config.DefaultStreamCreated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Periodical creating the default index set if it doesn't exist.
 */
public class DefaultIndexSetMigrationPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultIndexSetMigrationPeriodical.class);

    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final Map<String, Provider<RotationStrategy>> rotationStrategies;
    private final Map<String, Provider<RetentionStrategy>> retentionStrategies;
    private final IndexSetService indexSetService;
    private final ClusterEventBus clusterEventBus;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public DefaultIndexSetMigrationPeriodical(final ElasticsearchConfiguration elasticsearchConfiguration,
                                              final Map<String, Provider<RotationStrategy>> rotationStrategies,
                                              final Map<String, Provider<RetentionStrategy>> retentionStrategies,
                                              final IndexSetService indexSetService,
                                              final ClusterEventBus clusterEventBus,
                                              final ClusterConfigService clusterConfigService) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.rotationStrategies = requireNonNull(rotationStrategies);
        this.retentionStrategies = requireNonNull(retentionStrategies);
        this.indexSetService = indexSetService;
        this.clusterEventBus = clusterEventBus;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public void doRun() {
        final IndexSetConfig config = IndexSetConfig.create(
                "Default",
                "Default index set",
                elasticsearchConfiguration.getIndexPrefix(),
                elasticsearchConfiguration.getShards(),
                elasticsearchConfiguration.getReplicas(),
                getRotationStrategyConfig().orElse(MessageCountRotationStrategyConfig.createDefault()),
                getRetentionStrategyConfig().orElse(DeletionRetentionStrategyConfig.createDefault()),
                ZonedDateTime.now(ZoneOffset.UTC)
        );

        final IndexSetConfig savedConfig = indexSetService.save(config);
        clusterConfigService.write(DefaultIndexSetCreated.create());

        LOG.debug("Successfully created default index set: {}", savedConfig);
    }

    private Optional<RotationStrategyConfig> getRotationStrategyConfig() {
        final IndexManagementConfig indexManagementConfig = clusterConfigService.get(IndexManagementConfig.class);
        if (indexManagementConfig == null) {
            return Optional.empty();
        }

        final String strategyName = indexManagementConfig.rotationStrategy();
        final Provider<RotationStrategy> provider = rotationStrategies.get(strategyName);
        if (provider == null) {
            return Optional.empty();
        }

        final RotationStrategy rotationStrategy = provider.get();
        @SuppressWarnings("unchecked")
        final Class<RotationStrategyConfig> configClass = (Class<RotationStrategyConfig>) rotationStrategy.configurationClass();

        return Optional.ofNullable(clusterConfigService.get(configClass));
    }

    private Optional<RetentionStrategyConfig> getRetentionStrategyConfig() {
        final IndexManagementConfig indexManagementConfig = clusterConfigService.get(IndexManagementConfig.class);
        if (indexManagementConfig == null) {
            return Optional.empty();
        }

        final String strategyName = indexManagementConfig.retentionStrategy();
        final Provider<RetentionStrategy> provider = retentionStrategies.get(strategyName);
        if (provider == null) {
            return Optional.empty();
        }

        final RetentionStrategy retentionStrategy = provider.get();
        @SuppressWarnings("unchecked")
        final Class<RetentionStrategyConfig> configClass = (Class<RetentionStrategyConfig>) retentionStrategy.configurationClass();

        return Optional.ofNullable(clusterConfigService.get(configClass));
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
        return clusterConfigService.get(DefaultIndexSetCreated.class) == null;
    }

    @Override
    public boolean isDaemon() {
        return false;
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
