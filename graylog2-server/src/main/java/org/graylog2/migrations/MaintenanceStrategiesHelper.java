package org.graylog2.migrations;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.graylog2.configuration.ElasticsearchConfiguration;
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
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
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
}
