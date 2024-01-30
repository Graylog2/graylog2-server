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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;

import java.util.concurrent.TimeUnit;

import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_DATA_TIERING;

/**
 * In-database configuration (via ClusterConfigService) for index set
 * The values in this class are initialized from {@link ElasticsearchConfiguration} configuration properties
 * to allow users to specify defaults for default system indices on the first boot of the Graylog server.
 */
@JsonAutoDetect
@AutoValue
@JsonDeserialize(builder = AutoValue_IndexSetsDefaultConfiguration.Builder.class)
public abstract class IndexSetsDefaultConfiguration implements PluginConfigBean {

    public static final String INDEX_ANALYZER = "index_analyzer";
    public static final String SHARDS = "shards";
    public static final String REPLICAS = "replicas";
    public static final String INDEX_OPTIMIZATION_DISABLED = "index_optimization_disabled";
    public static final String INDEX_OPTIMIZATION_MAX_SEGMENTS = "index_optimization_max_num_segments";
    public static final String FIELD_TYPE_REFRESH_INTERVAL = "field_type_refresh_interval";
    public static final String FIELD_TYPE_REFRESH_INTERVAL_UNIT = "field_type_refresh_interval_unit";
    public static final String ROTATION_STRATEGY_CLASS = "rotation_strategy_class";
    public static final String ROTATION_STRATEGY_CONFIG = "rotation_strategy_config";
    public static final String ROTATION_STRATEGY = "rotation_strategy"; // alias for rotation_strategy_config
    public static final String RETENTION_STRATEGY_CLASS = "retention_strategy_class";
    public static final String RETENTION_STRATEGY_CONFIG = "retention_strategy_config";
    public static final String RETENTION_STRATEGY = "retention_strategy"; // alias for retention_strategy_config

    public static Builder builder() {
        return new AutoValue_IndexSetsDefaultConfiguration.Builder();
    }

    @NotBlank
    @JsonProperty(INDEX_ANALYZER)
    public abstract String indexAnalyzer();

    @Min(1)
    @JsonProperty(SHARDS)
    public abstract int shards();

    @Min(0)
    @JsonProperty(REPLICAS)
    public abstract int replicas();

    @Min(1)
    @JsonProperty(INDEX_OPTIMIZATION_MAX_SEGMENTS)
    public abstract int indexOptimizationMaxNumSegments();

    @JsonProperty(INDEX_OPTIMIZATION_DISABLED)
    public abstract boolean indexOptimizationDisabled();

    @Min(0)
    @JsonProperty(FIELD_TYPE_REFRESH_INTERVAL)
    public abstract long fieldTypeRefreshInterval();

    @JsonProperty(FIELD_TYPE_REFRESH_INTERVAL_UNIT)
    public abstract TimeUnit fieldTypeRefreshIntervalUnit();

    /**
     * The property names of rotation/retention settings must match those specified on
     * {@link org.graylog2.indexer.indexset.IndexSetConfig}, since shared UI components are used.
     */
    @NotBlank
    @JsonProperty(ROTATION_STRATEGY_CLASS)
    public abstract String rotationStrategyClass();

    @NotNull
    @JsonProperty(ROTATION_STRATEGY_CONFIG)
    public abstract RotationStrategyConfig rotationStrategyConfig();

    @NotNull
    @JsonProperty(ROTATION_STRATEGY)
    // alias for ROTATION_STRATEGY_CONFIG
    public RotationStrategyConfig rotationStrategy() {
        return rotationStrategyConfig();
    }

    @NotBlank
    @JsonProperty(RETENTION_STRATEGY_CLASS)
    public abstract String retentionStrategyClass();

    @NotNull
    @JsonProperty(RETENTION_STRATEGY_CONFIG)
    public abstract RetentionStrategyConfig retentionStrategyConfig();

    @NotNull
    @JsonProperty(RETENTION_STRATEGY)
    // alias for RETENTION_STRATEGY_CONFIG
    public RetentionStrategyConfig retentionStrategy() {
        return retentionStrategyConfig();
    }

    @NotNull
    @JsonProperty(FIELD_DATA_TIERING)
    public abstract DataTieringConfig dataTiering();

    public abstract Builder toBuilder();

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

        @JsonProperty(ROTATION_STRATEGY)
        // alias for ROTATION_STRATEGY_CONFIG
        public Builder rotationStrategy(RotationStrategyConfig rotationStrategyConfig) {
            return rotationStrategyConfig(rotationStrategyConfig);
        }

        @JsonProperty(RETENTION_STRATEGY_CLASS)
        public abstract Builder retentionStrategyClass(String retentionStrategyClass);

        @JsonProperty(RETENTION_STRATEGY_CONFIG)
        public abstract Builder retentionStrategyConfig(RetentionStrategyConfig retentionStrategyConfig);

        @JsonProperty(RETENTION_STRATEGY)
        // alias for RETENTION_STRATEGY_CONFIG
        public Builder retentionStrategy(RetentionStrategyConfig retentionStrategyConfig) {
            return retentionStrategyConfig(retentionStrategyConfig);
        }

        @JsonProperty(FIELD_DATA_TIERING)
        public abstract Builder dataTiering(DataTieringConfig dataTiering);

        public abstract IndexSetsDefaultConfiguration build();
    }
}
