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
package org.graylog2.indexer.management;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.mongodb.lang.Nullable;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;

import java.util.concurrent.TimeUnit;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class IndexManagementConfig {

    // Fields
    public static final String INDEX_PREFIX = "index_prefix";
    public static final String INDEX_ANALYZER = "index_analyzer";
    public static final String SHARDS = "shards";
    public static final String REPLICAS = "replicas";
    public static final String INDEX_OPTIMIZATION_DISABLED = "index_optimization_disabled";
    public static final String INDEX_OPTIMIZATION_MAX_SEGMENTS = "index_optimization_max_num_segments";
    public static final String FIELD_TYPE_REFRESH_INTERVAL = "field_type_refresh_interval";
    public static final String FIELD_TYPE_REFRESH_INTERVAL_UNIT = "field_type_refresh_interval_unit";
    public static final String ROTATION_STRATEGY = "rotation_strategy";
    public static final String RETENTION_STRATEGY = "retention_strategy";
    public static final String ROTATION_STRATEGY_CONFIG = "rotation_strategy_config";
    public static final String RETENTION_STRATEGY_CONFIG = "retention_strategy_config";

    @Nullable
    @JsonProperty(INDEX_PREFIX)
    public abstract String indexPrefix();

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

    @JsonProperty(ROTATION_STRATEGY)
    public abstract String retentionStrategy();

    @JsonProperty(RETENTION_STRATEGY)
    public abstract String rotationStrategy();

    @JsonProperty(ROTATION_STRATEGY_CONFIG)
    public abstract RotationStrategyConfig rotationStrategyConfig();

    @JsonProperty(RETENTION_STRATEGY_CONFIG)
    public abstract RetentionStrategyConfig retentionStrategyConfig();

    public static IndexManagementConfig.Builder builder() {
        return new AutoValue_IndexManagementConfig.Builder();
    }

    @JsonCreator
    public static IndexManagementConfig create(@JsonProperty(ROTATION_STRATEGY) String rotationStrategy,
                                               @JsonProperty(RETENTION_STRATEGY) String retentionStrategy) {
        return IndexManagementConfig.builder()
                .rotationStrategy(rotationStrategy)
                .retentionStrategy(retentionStrategy)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(INDEX_PREFIX)
        public abstract Builder indexPrefix(String indexPrefix);

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

        @JsonProperty(ROTATION_STRATEGY)
        public abstract Builder rotationStrategy(String rotationStrategy);

        @JsonProperty(RETENTION_STRATEGY)
        public abstract Builder retentionStrategy(String retentionStrategy);

        @JsonProperty(ROTATION_STRATEGY_CONFIG)
        public abstract Builder rotationStrategyConfig(RotationStrategyConfig rotationStrategyConfig);

        @JsonProperty(RETENTION_STRATEGY_CONFIG)
        public abstract Builder retentionStrategyConfig(RetentionStrategyConfig retentionStrategyConfig);

        public abstract IndexManagementConfig build();
    }
}
