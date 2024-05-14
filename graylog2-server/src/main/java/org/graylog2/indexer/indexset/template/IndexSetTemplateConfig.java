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
package org.graylog2.indexer.indexset.template;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.indexer.indexset.SimpleIndexSetConfig;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.joda.time.Duration;

import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_TYPE_REFRESH_INTERVAL;

@JsonAutoDetect
@AutoValue
@JsonDeserialize(builder = IndexSetTemplateConfig.Builder.class)
public abstract class IndexSetTemplateConfig implements SimpleIndexSetConfig {

    public static final String FIELD_USE_LEGACY_ROTATION = "use_legacy_rotation";

    public static Builder builder() {
        return Builder.create();
    }

    @NotBlank
    @JsonProperty(FIELD_INDEX_ANALYZER)
    public abstract String indexAnalyzer();

    @NotNull
    @JsonProperty(value = FIELD_USE_LEGACY_ROTATION)
    public abstract Boolean useLegacyRotation();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_IndexSetTemplateConfig.Builder()
                    .useLegacyRotation(false);
        }

        @JsonProperty(FIELD_INDEX_ANALYZER)
        public abstract Builder indexAnalyzer(String indexAnalyzer);

        @JsonProperty(FIELD_SHARDS)
        public abstract Builder shards(int shards);

        @JsonProperty(FIELD_REPLICAS)
        public abstract Builder replicas(int replicas);

        @JsonProperty(FIELD_INDEX_OPTIMIZATION_MAX_NUM_SEGMENTS)
        public abstract Builder indexOptimizationMaxNumSegments(int indexOptimizationMaxNumSegments);

        @JsonProperty(FIELD_INDEX_OPTIMIZATION_DISABLED)
        public abstract Builder indexOptimizationDisabled(boolean indexOptimizationDisabled);

        @JsonProperty(FIELD_TYPE_REFRESH_INTERVAL)
        public abstract Builder fieldTypeRefreshInterval(Duration fieldTypeRefreshInterval);

        @JsonProperty(FIELD_ROTATION_STRATEGY_CLASS)
        public abstract Builder rotationStrategyClass(String rotationStrategyClass);

        @JsonProperty(FIELD_ROTATION_STRATEGY)
        public abstract Builder rotationStrategy(RotationStrategyConfig rotationStrategyConfig) ;

        @JsonProperty(FIELD_RETENTION_STRATEGY_CLASS)
        public abstract Builder retentionStrategyClass(String retentionStrategyClass);

        @JsonProperty(FIELD_RETENTION_STRATEGY)
        public abstract Builder retentionStrategy(RetentionStrategyConfig retentionStrategyConfig);

        @JsonProperty(FIELD_DATA_TIERING)
        public abstract Builder dataTiering(DataTieringConfig dataTiering);

        @JsonProperty(FIELD_USE_LEGACY_ROTATION)
        public abstract Builder useLegacyRotation(boolean useLegacyRotation);

        public abstract IndexSetTemplateConfig build();
    }
}
