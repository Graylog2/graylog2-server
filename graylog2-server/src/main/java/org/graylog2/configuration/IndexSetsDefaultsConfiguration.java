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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.mongodb.lang.Nullable;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategy;
import org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategyConfig;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;

import java.util.concurrent.TimeUnit;

/**
 * In-database configuration (via ClusterConfigService) for index set defaults.
 * The values in this class are initialized from {@link ElasticsearchConfiguration} configuration properties
 * to allow users to specify defaults for default system indices on the first boot of the Graylog server.
 */
@JsonAutoDetect
@AutoValue
@JsonDeserialize(builder = AutoValue_IndexSetsDefaultsConfiguration.Builder.class)
public abstract class IndexSetsDefaultsConfiguration implements PluginConfigBean {

    // Defaults
    public static final String DEFAULT_INDEX_ANALYZER = "standard";
    public static final Integer DEFAULT_SHARDS = 1;
    public static final Integer DEFAULT_REPLICAS = 0;
    public static final boolean DEFAULT_INDEX_OPTIMIZATION_DISABLED = false;
    public static final Integer DEFAULT_INDEX_OPTIMIZATION_MAX_SEGMENTS = 1;
    public static final long DEFAULT_FIELD_TYPE_REFRESH_INTERVAL = 5;
    public static final TimeUnit DEFAULT_FIELD_TYPE_REFRESH_INTERVAL_UNIT = TimeUnit.SECONDS;
    public static final String DEFAULT_ROTATION_STRATEGY_CLASS = SizeBasedRotationStrategy.class.getCanonicalName();
    public static final RotationStrategyConfig DEFAULT_ROTATION_STRATEGY_CONFIG = SizeBasedRotationStrategyConfig.createDefault();
    public static final String DEFAULT_RETENTION_STRATEGY_CLASS = DeletionRetentionStrategy.class.getCanonicalName();
    public static final RetentionStrategyConfig DEFAULT_RETENTION_STRATEGY_CONFIG = DeletionRetentionStrategyConfig.createDefault();

    // Fields
    public static final String INDEX_PREFIX = "index_prefix";
    public static final String INDEX_ANALYZER = "index_analyzer";
    public static final String SHARDS = "shards";
    public static final String REPLICAS = "replicas";
    public static final String INDEX_OPTIMIZATION_DISABLED = "index_optimization_disabled";
    public static final String INDEX_OPTIMIZATION_MAX_SEGMENTS = "index_optimization_max_num_segments";
    public static final String FIELD_TYPE_REFRESH_INTERVAL = "field_type_refresh_interval";
    public static final String FIELD_TYPE_REFRESH_INTERVAL_UNIT = "field_type_refresh_interval_unit";
    public static final String ROTATION_STRATEGY_CLASS = "rotation_strategy_class";
    public static final String ROTATION_STRATEGY_CONFIG = "rotation_strategy_config";
    public static final String RETENTION_STRATEGY_CLASS = "retention_strategy_class";
    public static final String RETENTION_STRATEGY_CONFIG = "retention_strategy_config";

    @JsonProperty(INDEX_ANALYZER)
    public abstract String indexAnalyzer();

    @JsonProperty(SHARDS)
    public abstract int shards();

    @JsonProperty(REPLICAS)
    public abstract int replicas();

    @JsonProperty(INDEX_OPTIMIZATION_MAX_SEGMENTS)
    public abstract int indexOptimizationMaxNumSegments();

    @JsonProperty(INDEX_OPTIMIZATION_DISABLED)
    public abstract boolean indexOptimizationDisabled();

    @JsonProperty(FIELD_TYPE_REFRESH_INTERVAL)
    public abstract long fieldTypeRefreshInterval();

    @JsonProperty(FIELD_TYPE_REFRESH_INTERVAL_UNIT)
    public abstract TimeUnit fieldTypeRefreshIntervalUnit();

    /**
     * The properties of the rotation/retention settings must match those specified on
     * {@link org.graylog2.indexer.indexset.IndexSetConfig}, since shared UI components are used.
     */
    @JsonProperty(ROTATION_STRATEGY_CLASS)
    public abstract String rotationStrategyClass();

    @JsonProperty(ROTATION_STRATEGY_CONFIG)
    public abstract RotationStrategyConfig rotationStrategyConfig();

    @JsonProperty(RETENTION_STRATEGY_CLASS)
    public abstract String retentionStrategyClass();

    @JsonProperty(RETENTION_STRATEGY_CONFIG)
    public abstract RetentionStrategyConfig retentionStrategyConfig();

    public static Builder builder() {
        return new AutoValue_IndexSetsDefaultsConfiguration.Builder()
                .indexAnalyzer(DEFAULT_INDEX_ANALYZER)
                .shards(DEFAULT_SHARDS)
                .replicas(DEFAULT_REPLICAS)
                .indexOptimizationDisabled(DEFAULT_INDEX_OPTIMIZATION_DISABLED)
                .indexOptimizationMaxNumSegments(DEFAULT_INDEX_OPTIMIZATION_MAX_SEGMENTS)
                .fieldTypeRefreshInterval(DEFAULT_FIELD_TYPE_REFRESH_INTERVAL)
                .fieldTypeRefreshIntervalUnit(DEFAULT_FIELD_TYPE_REFRESH_INTERVAL_UNIT)
                .rotationStrategyClass(DEFAULT_ROTATION_STRATEGY_CLASS)
                .rotationStrategyConfig(DEFAULT_ROTATION_STRATEGY_CONFIG)
                .retentionStrategyClass(DEFAULT_RETENTION_STRATEGY_CLASS)
                .retentionStrategyConfig(DEFAULT_RETENTION_STRATEGY_CONFIG);
    }

    public static IndexSetsDefaultsConfiguration createDefault() {
        return builder().build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(INDEX_ANALYZER)
        public abstract Builder indexAnalyzer(String indexAnalyzer);

        @JsonProperty(SHARDS)
        public abstract Builder shards(int shards);

        @JsonProperty(REPLICAS)
        public abstract Builder replicas(int replicas);

        @JsonProperty(INDEX_OPTIMIZATION_MAX_SEGMENTS)
        public abstract Builder indexOptimizationMaxNumSegments(int indexOptimizationMaxNumSegments);

        @JsonProperty(INDEX_OPTIMIZATION_DISABLED)
        public abstract Builder indexOptimizationDisabled(boolean indexOptimizationDisabled);

        @JsonProperty(FIELD_TYPE_REFRESH_INTERVAL)
        public abstract Builder fieldTypeRefreshInterval(long fieldTypeRefreshInterval);

        @JsonProperty(FIELD_TYPE_REFRESH_INTERVAL_UNIT)
        public abstract Builder fieldTypeRefreshIntervalUnit(TimeUnit fieldTypeRefreshIntervalUnit);

        @JsonProperty(ROTATION_STRATEGY_CLASS)
        public abstract Builder rotationStrategyClass(String rotationStrategyClass);

        @JsonProperty(ROTATION_STRATEGY_CONFIG)
        public abstract Builder rotationStrategyConfig(RotationStrategyConfig rotationStrategyConfig);

        @JsonProperty(RETENTION_STRATEGY_CLASS)
        public abstract Builder retentionStrategyClass(String retentionStrategyClass);

        @JsonProperty(RETENTION_STRATEGY_CONFIG)
        public abstract Builder retentionStrategyConfig(RetentionStrategyConfig retentionStrategyConfig);

        public abstract IndexSetsDefaultsConfiguration build();
    }
}
