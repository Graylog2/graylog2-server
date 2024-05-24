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
package org.graylog2.indexer.indexset;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.joda.time.Duration;

import javax.annotation.Nullable;

public interface SimpleIndexSetConfig {

    Duration DEFAULT_FIELD_TYPE_REFRESH_INTERVAL = Duration.standardSeconds(5L);
    String FIELD_TITLE = "title";
    String FIELD_DESCRIPTION = "description";
    String FIELD_WRITABLE = "writable";
    String FIELD_SHARDS = "shards";
    String FIELD_REPLICAS = "replicas";
    String FIELD_ROTATION_STRATEGY_CLASS = "rotation_strategy_class";
    String FIELD_ROTATION_STRATEGY = "rotation_strategy";
    String FIELD_RETENTION_STRATEGY_CLASS = "retention_strategy_class";
    String FIELD_RETENTION_STRATEGY = "retention_strategy";
    String FIELD_INDEX_ANALYZER = "index_analyzer";
    String FIELD_INDEX_OPTIMIZATION_MAX_NUM_SEGMENTS = "index_optimization_max_num_segments";
    String FIELD_INDEX_OPTIMIZATION_DISABLED = "index_optimization_disabled";
    String FIELD_DATA_TIERING = "data_tiering";
    String INDEX_PREFIX_REGEX = "^[a-z0-9][a-z0-9_+-]*$";
    String FIELD_INDEX_PREFIX = "index_prefix";
    String FIELD_CREATION_DATE = "creation_date";
    String FIELD_INDEX_TEMPLATE_TYPE = "index_template_type";
    String FIELD_TYPE_REFRESH_INTERVAL = "field_type_refresh_interval";
    String FIELD_USE_LEGACY_ROTATION = "use_legacy_rotation";
    String FIELD_PROFILE_ID = "field_type_profile";

    @Min(1)
    @JsonProperty(FIELD_SHARDS)
    int shards();

    @Min(0)
    @JsonProperty(FIELD_REPLICAS)
    int replicas();

    @Min(1)
    @JsonProperty(FIELD_INDEX_OPTIMIZATION_MAX_NUM_SEGMENTS)
    int indexOptimizationMaxNumSegments();

    @JsonProperty(FIELD_INDEX_OPTIMIZATION_DISABLED)
    boolean indexOptimizationDisabled();

    @JsonProperty(FIELD_TYPE_REFRESH_INTERVAL)
    Duration fieldTypeRefreshInterval();

    @Nullable
    @JsonProperty(FIELD_ROTATION_STRATEGY_CLASS)
    String rotationStrategyClass();

    @Nullable
    @JsonProperty(FIELD_ROTATION_STRATEGY)
    RotationStrategyConfig rotationStrategyConfig();

    @Nullable
    @JsonProperty(FIELD_RETENTION_STRATEGY_CLASS)
    String retentionStrategyClass();

    @Nullable
    @JsonProperty(FIELD_RETENTION_STRATEGY)
    RetentionStrategyConfig retentionStrategyConfig();

    @Nullable
    @JsonProperty(FIELD_DATA_TIERING)
    DataTieringConfig dataTieringConfig();

}
