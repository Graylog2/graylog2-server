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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.bson.types.ObjectId;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.management.IndexManagementConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.events.StreamsChangedEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Periodical creating the default stream if it doesn't exist.
 */
public class DefaultStreamMigrationPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultStreamMigrationPeriodical.class);

    private final StreamService streamService;
    private final StreamRuleService streamRuleService;
    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final Map<String, Provider<RotationStrategy>> rotationStrategies;
    private final Map<String, Provider<RetentionStrategy>> retentionStrategies;
    private final IndexSetService indexSetService;
    private final ClusterEventBus clusterEventBus;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public DefaultStreamMigrationPeriodical(final StreamService streamService,
                                            final StreamRuleService streamRuleService,
                                            final ElasticsearchConfiguration elasticsearchConfiguration,
                                            final Map<String, Provider<RotationStrategy>> rotationStrategies,
                                            final Map<String, Provider<RetentionStrategy>> retentionStrategies,
                                            final IndexSetService indexSetService,
                                            final ClusterEventBus clusterEventBus,
                                            final ClusterConfigService clusterConfigService) {
        this.streamService = streamService;
        this.streamRuleService = streamRuleService;
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.rotationStrategies = rotationStrategies;
        this.retentionStrategies = retentionStrategies;
        this.indexSetService = indexSetService;
        this.clusterEventBus = clusterEventBus;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public void doRun() {
        try {
            streamService.load(Stream.DEFAULT_STREAM_ID);
        } catch (NotFoundException ignored) {
            doRunDefaultStreamMigration();
        }

        if (clusterConfigService.get(IndexSetMigrated.class) == null) {
            doRunIndexSetMigration();
        }
    }

    @VisibleForTesting
    void doRunDefaultStreamMigration() {
        final ObjectId id = new ObjectId(Stream.DEFAULT_STREAM_ID);
        final Map<String, Object> fields = ImmutableMap.<String, Object>builder()
                .put(StreamImpl.FIELD_TITLE, "All messages")
                .put(StreamImpl.FIELD_DESCRIPTION, "Stream containing all messages")
                .put(StreamImpl.FIELD_DISABLED, false)
                .put(StreamImpl.FIELD_CREATED_AT, DateTime.now(DateTimeZone.UTC))
                .put(StreamImpl.FIELD_CREATOR_USER_ID, "local:admin")
                .put(StreamImpl.FIELD_MATCHING_TYPE, StreamImpl.MatchingType.DEFAULT.name())
                .put(StreamImpl.FIELD_DEFAULT_STREAM, true)
                .build();
        final Stream stream = new StreamImpl(id, fields, Collections.emptyList(), Collections.emptySet(), Collections.emptySet());

        try {
            streamService.save(stream);
            LOG.info("Successfully created default stream: {}", stream.getTitle());

            clusterEventBus.post(StreamsChangedEvent.create(stream.getId()));
        } catch (ValidationException e) {
            LOG.error("Couldn't create default stream", e);
        }
    }

    @VisibleForTesting
    void doRunIndexSetMigration() {
        final IndexManagementConfig indexManagementConfig = clusterConfigService.get(IndexManagementConfig.class);
        checkState(indexManagementConfig != null, "Couldn't find index management configuration");

        final RotationStrategyConfig rotationStrategyConfig = getRotationStrategyConfig(indexManagementConfig);
        final RetentionStrategyConfig retentionStrategyConfig = getRetentionStrategyConfig(indexManagementConfig);

        final IndexSetConfig indexSetConfig = IndexSetConfig.builder()
                .title("Default index set")
                .description("The Graylog default index set")
                .indexPrefix(elasticsearchConfiguration.getIndexPrefix())
                .shards(elasticsearchConfiguration.getShards())
                .replicas(elasticsearchConfiguration.getReplicas())
                .rotationStrategy(rotationStrategyConfig)
                .retentionStrategy(retentionStrategyConfig)
                .creationDate(ZonedDateTime.now(ZoneOffset.UTC))
                .build();

        final IndexSetConfig savedConfig = indexSetService.save(indexSetConfig);
        clusterConfigService.write(IndexSetMigrated.create());

        LOG.info("Successfully migrated index settings (database ID <{}>)", savedConfig.id());
    }

    private RetentionStrategyConfig getRetentionStrategyConfig(IndexManagementConfig indexManagementConfig) {
        final String strategyName = indexManagementConfig.retentionStrategy();
        final Provider<RetentionStrategy> provider = retentionStrategies.get(strategyName);
        checkState(provider != null, "Couldn't retrieve retention strategy provider for <" + strategyName + ">");
        final RetentionStrategy strategy = provider.get();
        @SuppressWarnings("unchecked")
        final Class<RetentionStrategyConfig> configClass = (Class<RetentionStrategyConfig>) strategy.configurationClass();
        final RetentionStrategyConfig config = clusterConfigService.get(configClass);
        return firstNonNull(config, strategy.defaultConfiguration());
    }

    private RotationStrategyConfig getRotationStrategyConfig(IndexManagementConfig indexManagementConfig) {
        final String strategyName = indexManagementConfig.rotationStrategy();
        final Provider<RotationStrategy> provider = rotationStrategies.get(strategyName);
        checkState(provider != null, "Couldn't retrieve rotation strategy provider for <" + strategyName + ">");
        final RotationStrategy strategy = provider.get();
        @SuppressWarnings("unchecked")
        final Class<RotationStrategyConfig> configClass = (Class<RotationStrategyConfig>) strategy.configurationClass();
        final RotationStrategyConfig config = clusterConfigService.get(configClass);
        return firstNonNull(config, strategy.defaultConfiguration());
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

    @JsonAutoDetect
    @AutoValue
    static abstract class IndexSetMigrated {
        @JsonCreator
        public static IndexSetMigrated create() {
            return new AutoValue_DefaultStreamMigrationPeriodical_IndexSetMigrated();
        }
    }
}
