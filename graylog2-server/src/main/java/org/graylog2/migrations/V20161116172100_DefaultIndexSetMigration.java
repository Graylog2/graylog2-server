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
package org.graylog2.migrations;

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.DefaultIndexSetConfig;
import org.graylog2.indexer.indexset.DefaultIndexSetCreated;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.events.IndexSetCreatedEvent;
import org.graylog2.indexer.management.IndexManagementConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

/**
 * Migration creating the default index set from the legacy settings.
 */
public class V20161116172100_DefaultIndexSetMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20161116172100_DefaultIndexSetMigration.class);

    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final Map<String, Provider<RotationStrategy>> rotationStrategies;
    private final Map<String, Provider<RetentionStrategy>> retentionStrategies;
    private final IndexSetService indexSetService;
    private final ClusterConfigService clusterConfigService;
    private final ClusterEventBus clusterEventBus;

    @Inject
    public V20161116172100_DefaultIndexSetMigration(final ElasticsearchConfiguration elasticsearchConfiguration,
                                                    final Map<String, Provider<RotationStrategy>> rotationStrategies,
                                                    final Map<String, Provider<RetentionStrategy>> retentionStrategies,
                                                    final IndexSetService indexSetService,
                                                    final ClusterConfigService clusterConfigService,
                                                    final ClusterEventBus clusterEventBus) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.rotationStrategies = requireNonNull(rotationStrategies);
        this.retentionStrategies = requireNonNull(retentionStrategies);
        this.indexSetService = indexSetService;
        this.clusterConfigService = clusterConfigService;
        this.clusterEventBus = clusterEventBus;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2016-11-16T17:21:00Z");
    }

    @Override
    public void upgrade() {
        // Do not run again if the migration marker can be found in the database.
        if (clusterConfigService.get(DefaultIndexSetCreated.class) != null) {
            return;
        }

        final IndexManagementConfig indexManagementConfig = clusterConfigService.get(IndexManagementConfig.class);

        checkState(indexManagementConfig != null, "Couldn't find index management configuration");

        final IndexSetConfig config = IndexSetConfig.builder()
                .title("Default index set")
                .description("The Graylog default index set")
                .indexPrefix(elasticsearchConfiguration.getIndexPrefix())
                .shards(elasticsearchConfiguration.getShards())
                .replicas(elasticsearchConfiguration.getReplicas())
                .rotationStrategy(getRotationStrategyConfig(indexManagementConfig))
                .retentionStrategy(getRetentionStrategyConfig(indexManagementConfig))
                .creationDate(ZonedDateTime.now(ZoneOffset.UTC))
                .indexAnalyzer(elasticsearchConfiguration.getAnalyzer())
                .indexTemplateName(elasticsearchConfiguration.getTemplateName())
                .indexOptimizationMaxNumSegments(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(elasticsearchConfiguration.isDisableIndexOptimization())
                .build();

        final IndexSetConfig savedConfig = indexSetService.save(config);
        clusterConfigService.write(DefaultIndexSetConfig.create(savedConfig.id()));
        clusterConfigService.write(DefaultIndexSetCreated.create());

        // Publish event to cluster event bus so the stream router will reload.
        clusterEventBus.post(IndexSetCreatedEvent.create(savedConfig));

        LOG.debug("Successfully created default index set: {}", savedConfig);
    }

    private RotationStrategyConfig getRotationStrategyConfig(IndexManagementConfig indexManagementConfig) {
        final String strategyName = indexManagementConfig.rotationStrategy();
        final Provider<RotationStrategy> provider = rotationStrategies.get(strategyName);
        checkState(provider != null, "Couldn't retrieve rotation strategy provider for <" + strategyName + ">");

        final RotationStrategy rotationStrategy = provider.get();
        @SuppressWarnings("unchecked")
        final Class<RotationStrategyConfig> configClass = (Class<RotationStrategyConfig>) rotationStrategy.configurationClass();

        final RotationStrategyConfig rotationStrategyConfig = clusterConfigService.get(configClass);
        checkState(rotationStrategyConfig != null, "Couldn't retrieve rotation strategy config for <" + strategyName + ">");

        return rotationStrategyConfig;
    }

    private RetentionStrategyConfig getRetentionStrategyConfig(IndexManagementConfig indexManagementConfig) {
        final String strategyName = indexManagementConfig.retentionStrategy();
        final Provider<RetentionStrategy> provider = retentionStrategies.get(strategyName);
        checkState(provider != null, "Couldn't retrieve retention strategy provider for <" + strategyName + ">");

        final RetentionStrategy retentionStrategy = provider.get();
        @SuppressWarnings("unchecked")
        final Class<RetentionStrategyConfig> configClass = (Class<RetentionStrategyConfig>) retentionStrategy.configurationClass();

        final RetentionStrategyConfig retentionStrategyConfig = clusterConfigService.get(configClass);
        checkState(retentionStrategyConfig!= null, "Couldn't retrieve retention strategy config for <" + strategyName + ">");

        return retentionStrategyConfig;
    }
}
