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
package org.graylog2.rest.resources.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.SimpleIndexSetConfig;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.validation.SizeInBytes;
import org.joda.time.Duration;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class IndexSetSummary implements SimpleIndexSetConfig {
    public static final String FIELD_DEFAULT = "default";
    public static final String FIELD_CAN_BE_DEFAULT = "can_be_default";
    public static final String FIELD_DATA_TIERING_STATUS = "data_tiering_status";

    @JsonCreator
    public static IndexSetSummary create(@JsonProperty("id") @Nullable String id,
                                         @JsonProperty(FIELD_TITLE) @NotBlank String title,
                                         @JsonProperty(FIELD_DESCRIPTION) @Nullable String description,
                                         @JsonProperty(FIELD_DEFAULT) boolean isDefault,
                                         @JsonProperty(FIELD_WRITABLE) boolean isWritable,
                                         @JsonProperty(FIELD_CAN_BE_DEFAULT) boolean canBeDefault,
                                         @JsonProperty(FIELD_INDEX_PREFIX) @Pattern(regexp = INDEX_PREFIX_REGEX) String indexPrefix,
                                         @JsonProperty(FIELD_SHARDS) @Min(1) int shards,
                                         @JsonProperty(FIELD_REPLICAS) @Min(0) int replicas,
                                         @JsonProperty(FIELD_ROTATION_STRATEGY_CLASS) @NotNull String rotationStrategyClass,
                                         @JsonProperty(FIELD_ROTATION_STRATEGY) @NotNull RotationStrategyConfig rotationStrategy,
                                         @JsonProperty(FIELD_RETENTION_STRATEGY_CLASS) @NotNull String retentionStrategyClass,
                                         @JsonProperty(FIELD_RETENTION_STRATEGY) @NotNull RetentionStrategyConfig retentionStrategy,
                                         @JsonProperty(FIELD_CREATION_DATE) @Nullable ZonedDateTime creationDate,
                                         @JsonProperty(FIELD_INDEX_ANALYZER) @NotBlank String indexAnalyzer,
                                         @JsonProperty(FIELD_INDEX_OPTIMIZATION_MAX_NUM_SEGMENTS) @Min(1L) int indexOptimizationMaxNumSegments,
                                         @JsonProperty(FIELD_INDEX_OPTIMIZATION_DISABLED) boolean indexOptimizationDisabled,
                                         @JsonProperty(FIELD_TYPE_REFRESH_INTERVAL) Duration fieldTypeRefreshInterval,
                                         @JsonProperty(FIELD_INDEX_TEMPLATE_TYPE) @Nullable String templateType,
                                         @JsonProperty(FIELD_PROFILE_ID) @Nullable String fieldTypeProfile,
                                         @JsonProperty(FIELD_DATA_TIERING) @Nullable DataTieringConfig dataTiering,
                                         @JsonProperty(FIELD_USE_LEGACY_ROTATION) @Nullable Boolean useLegacyRotation,
                                         @JsonProperty(FIELD_DATA_TIERING_STATUS) @Nullable DataTieringStatus dataTieringStatus) {
        if (Objects.isNull(creationDate)) {
            creationDate = ZonedDateTime.now();
        }
        return new AutoValue_IndexSetSummary(shards, replicas, rotationStrategyClass, rotationStrategy, retentionStrategyClass,
                retentionStrategy, dataTiering, id, title, description, isDefault, canBeDefault, isWritable, indexPrefix,
                creationDate, indexAnalyzer, indexOptimizationMaxNumSegments, indexOptimizationDisabled, fieldTypeRefreshInterval,
                Optional.ofNullable(templateType), fieldTypeProfile, Objects.isNull(useLegacyRotation) || useLegacyRotation,
                dataTieringStatus);
    }

    public static IndexSetSummary fromIndexSetConfig(IndexSetConfig indexSet, boolean isDefault) {
        return fromIndexSetConfig(indexSet, isDefault, null);
    }

    public static IndexSetSummary fromIndexSetConfig(IndexSetConfig indexSet, boolean isDefault, DataTieringStatus dataTieringStatus) {
        return create(
                indexSet.id(),
                indexSet.title(),
                indexSet.description(),
                isDefault,
                indexSet.isWritable(),
                indexSet.isRegularIndex(),
                indexSet.indexPrefix(),
                indexSet.shards(),
                indexSet.replicas(),
                indexSet.rotationStrategyClass(),
                indexSet.rotationStrategyConfig(),
                indexSet.retentionStrategyClass(),
                indexSet.retentionStrategyConfig(),
                indexSet.creationDate(),
                indexSet.indexAnalyzer(),
                indexSet.indexOptimizationMaxNumSegments(),
                indexSet.indexOptimizationDisabled(),
                indexSet.fieldTypeRefreshInterval(),
                indexSet.indexTemplateType().orElse(null),
                indexSet.fieldTypeProfile(),
                indexSet.dataTieringConfig(),
                indexSet.dataTieringConfig() == null,
                dataTieringStatus
        );
    }

    @JsonProperty("id")
    @Nullable
    public abstract String id();

    @JsonProperty(FIELD_TITLE)
    @NotBlank
    public abstract String title();

    @JsonProperty(FIELD_DESCRIPTION)
    @Nullable
    public abstract String description();

    @JsonProperty(FIELD_DEFAULT)
    public abstract boolean isDefault();

    @JsonProperty(FIELD_CAN_BE_DEFAULT)
    public abstract boolean canBeDefault();

    @JsonProperty(FIELD_WRITABLE)
    public abstract boolean isWritable();

    @JsonProperty(FIELD_INDEX_PREFIX)
    @Pattern(regexp = INDEX_PREFIX_REGEX)
    @SizeInBytes(message = "Index prefix must have a length in bytes between {min} and {max}", min = 1, max = 250)
    public abstract String indexPrefix();

    @JsonProperty(FIELD_CREATION_DATE)
    @NotNull
    public abstract ZonedDateTime creationDate();

    @JsonProperty(FIELD_INDEX_ANALYZER)
    @NotBlank
    public abstract String indexAnalyzer();

    @JsonProperty(FIELD_INDEX_OPTIMIZATION_MAX_NUM_SEGMENTS)
    @Min(1L)
    public abstract int indexOptimizationMaxNumSegments();

    @JsonProperty(FIELD_INDEX_OPTIMIZATION_DISABLED)
    public abstract boolean indexOptimizationDisabled();

    @JsonProperty(FIELD_TYPE_REFRESH_INTERVAL)
    public abstract Duration fieldTypeRefreshInterval();

    @JsonProperty(FIELD_INDEX_TEMPLATE_TYPE)
    public abstract Optional<String> templateType();

    @JsonProperty(FIELD_PROFILE_ID)
    @Nullable
    public abstract String fieldTypeProfile();

    @Nullable
    @JsonProperty(FIELD_USE_LEGACY_ROTATION)
    public abstract Boolean useLegacyRotation();

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(FIELD_DATA_TIERING_STATUS)
    public abstract DataTieringStatus dataTieringStatus();

    public IndexSetConfig toIndexSetConfig(boolean isRegular) {
        final IndexSetConfig.Builder builder = IndexSetConfig.builder()
                .id(id())
                .title(title())
                .description(description())
                .isWritable(isWritable())
                .isRegular(isRegular)
                .indexPrefix(indexPrefix())
                .shards(shards())
                .replicas(replicas())
                .rotationStrategyClass(rotationStrategyClass())
                .rotationStrategyConfig(rotationStrategyConfig())
                .retentionStrategyClass(retentionStrategyClass())
                .retentionStrategyConfig(retentionStrategyConfig())
                .creationDate(creationDate())
                .indexAnalyzer(indexAnalyzer())
                .indexTemplateName(indexPrefix() + "-template")
                .indexOptimizationMaxNumSegments(indexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(indexOptimizationDisabled())
                .fieldTypeRefreshInterval(fieldTypeRefreshInterval())
                .fieldTypeProfile(fieldTypeProfile())
                .dataTieringConfig(Boolean.FALSE.equals(useLegacyRotation()) ? dataTieringConfig() : null);

        final IndexSetConfig.Builder builderWithTemplateType = templateType().map(builder::indexTemplateType).orElse(builder);
        return builderWithTemplateType.build();
    }
}
