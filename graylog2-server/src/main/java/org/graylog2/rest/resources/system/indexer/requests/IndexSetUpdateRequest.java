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
package org.graylog2.rest.resources.system.indexer.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.validation.ValidObjectId;
import org.joda.time.Duration;

import javax.annotation.Nullable;

import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_DATA_TIERING;
import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_PROFILE_ID;
import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_RETENTION_STRATEGY;
import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_RETENTION_STRATEGY_CLASS;
import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_ROTATION_STRATEGY;
import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_ROTATION_STRATEGY_CLASS;
import static org.graylog2.rest.resources.system.indexer.responses.IndexSetSummary.FIELD_USE_LEGACY_ROTATION;

@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
public record IndexSetUpdateRequest(@JsonProperty("title") @NotBlank String title,
                                    @JsonProperty("description") @Nullable String description,
                                    @JsonProperty("writable") boolean isWritable,
                                    @JsonProperty("shards") @Min(1) int shards,
                                    @JsonProperty("replicas") @Min(0) int replicas,
                                    @JsonProperty(FIELD_ROTATION_STRATEGY_CLASS) @Nullable String rotationStrategyClass,
                                    @JsonProperty(FIELD_ROTATION_STRATEGY) @Nullable RotationStrategyConfig rotationStrategy,
                                    @JsonProperty(FIELD_RETENTION_STRATEGY_CLASS) @Nullable String retentionStrategyClass,
                                    @JsonProperty(FIELD_RETENTION_STRATEGY) @Nullable RetentionStrategyConfig retentionStrategy,
                                    @JsonProperty("index_optimization_max_num_segments") @Min(1L) int indexOptimizationMaxNumSegments,
                                    @JsonProperty("index_optimization_disabled") boolean indexOptimizationDisabled,
                                    @JsonProperty("field_type_refresh_interval") Duration fieldTypeRefreshInterval,
                                    @JsonProperty(FIELD_PROFILE_ID) @ValidObjectId @Nullable String fieldTypeProfile,
                                    @JsonProperty(FIELD_DATA_TIERING) @Nullable DataTieringConfig dataTiering,
                                    @JsonProperty(FIELD_USE_LEGACY_ROTATION) @Nullable Boolean useLegacyRotation) {


    public static IndexSetUpdateRequest fromIndexSetConfig(final IndexSetConfig indexSet) {
        return new IndexSetUpdateRequest(
                indexSet.title(),
                indexSet.description(),
                indexSet.isWritable(),
                indexSet.shards(),
                indexSet.replicas(),
                indexSet.rotationStrategyClass(),
                indexSet.rotationStrategy(),
                indexSet.retentionStrategyClass(),
                indexSet.retentionStrategy(),
                indexSet.indexOptimizationMaxNumSegments(),
                indexSet.indexOptimizationDisabled(),
                indexSet.fieldTypeRefreshInterval(),
                indexSet.fieldTypeProfile(),
                indexSet.dataTiering(),
                indexSet.dataTiering() == null);

    }

    public IndexSetConfig toIndexSetConfig(final String id, final IndexSetConfig oldConfig) {
        return IndexSetConfig.builder()
                .id(id)
                .title(title())
                .description(description())
                .isWritable(isWritable())
                .isRegular(oldConfig.isRegular().orElse(null))
                .indexPrefix(oldConfig.indexPrefix())
                .indexMatchPattern(oldConfig.indexMatchPattern())
                .indexWildcard(oldConfig.indexWildcard())
                .shards(shards())
                .replicas(replicas())
                .rotationStrategyClass(rotationStrategyClass())
                .rotationStrategy(rotationStrategy())
                .retentionStrategyClass(retentionStrategyClass())
                .retentionStrategy(retentionStrategy())
                .creationDate(oldConfig.creationDate())
                .indexAnalyzer(oldConfig.indexAnalyzer())
                .indexTemplateName(oldConfig.indexTemplateName())
                .indexTemplateType(oldConfig.indexTemplateType().orElse(null))
                .indexOptimizationMaxNumSegments(indexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(indexOptimizationDisabled())
                .fieldTypeRefreshInterval(fieldTypeRefreshInterval())
                .fieldTypeProfile(fieldTypeProfile())
                .customFieldMappings(oldConfig.customFieldMappings())
                .dataTiering(Boolean.FALSE.equals(useLegacyRotation()) ? dataTiering() : null)
                .build();
    }
}
