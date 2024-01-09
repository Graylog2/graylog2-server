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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.validation.SizeInBytes;
import org.joda.time.Duration;

import javax.annotation.Nullable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_DATA_TIERING;
import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_PROFILE_ID;
import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_RETENTION_STRATEGY;
import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_RETENTION_STRATEGY_CLASS;
import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_ROTATION_STRATEGY;
import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_ROTATION_STRATEGY_CLASS;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class IndexSetSummary {
    public static final String FIELD_USE_LEGACY_ROTATION = "use_legacy_rotation";

    @JsonCreator
    public static IndexSetSummary create(@JsonProperty("id") @Nullable String id,
                                         @JsonProperty("title") @NotBlank String title,
                                         @JsonProperty("description") @Nullable String description,
                                         @JsonProperty("default") boolean isDefault,
                                         @JsonProperty("writable") boolean isWritable,
                                         @JsonProperty("can_be_default") boolean canBeDefault,
                                         @JsonProperty("index_prefix") @Pattern(regexp = IndexSetConfig.INDEX_PREFIX_REGEX) String indexPrefix,
                                         @JsonProperty("shards") @Min(1) int shards,
                                         @JsonProperty("replicas") @Min(0) int replicas,
                                         @JsonProperty(FIELD_ROTATION_STRATEGY_CLASS) @NotNull String rotationStrategyClass,
                                         @JsonProperty(FIELD_ROTATION_STRATEGY) @NotNull RotationStrategyConfig rotationStrategy,
                                         @JsonProperty(FIELD_RETENTION_STRATEGY_CLASS) @NotNull String retentionStrategyClass,
                                         @JsonProperty(FIELD_RETENTION_STRATEGY) @NotNull RetentionStrategyConfig retentionStrategy,
                                         @JsonProperty("creation_date") @Nullable ZonedDateTime creationDate,
                                         @JsonProperty("index_analyzer") @NotBlank String indexAnalyzer,
                                         @JsonProperty("index_optimization_max_num_segments") @Min(1L) int indexOptimizationMaxNumSegments,
                                         @JsonProperty("index_optimization_disabled") boolean indexOptimizationDisabled,
                                         @JsonProperty("field_type_refresh_interval") Duration fieldTypeRefreshInterval,
                                         @JsonProperty("index_template_type") @Nullable String templateType,
                                         @JsonProperty(FIELD_PROFILE_ID) @Nullable String fieldTypeProfile,
                                         @JsonProperty(FIELD_DATA_TIERING) @Nullable DataTieringConfig dataTiering,
                                         @JsonProperty(FIELD_USE_LEGACY_ROTATION) Boolean userLegacyRotation) {
        if (Objects.isNull(creationDate)) {
            creationDate = ZonedDateTime.now();
        }
        return new AutoValue_IndexSetSummary(id, title, description, isDefault, canBeDefault,
                isWritable, indexPrefix, shards, replicas,
                rotationStrategyClass, rotationStrategy, retentionStrategyClass, retentionStrategy, creationDate,
                indexAnalyzer, indexOptimizationMaxNumSegments, indexOptimizationDisabled, fieldTypeRefreshInterval,
                Optional.ofNullable(templateType), fieldTypeProfile, dataTiering, userLegacyRotation);
    }

    public static IndexSetSummary fromIndexSetConfig(IndexSetConfig indexSet, boolean isDefault) {
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
                indexSet.rotationStrategy(),
                indexSet.retentionStrategyClass(),
                indexSet.retentionStrategy(),
                indexSet.creationDate(),
                indexSet.indexAnalyzer(),
                indexSet.indexOptimizationMaxNumSegments(),
                indexSet.indexOptimizationDisabled(),
                indexSet.fieldTypeRefreshInterval(),
                indexSet.indexTemplateType().orElse(null),
                indexSet.fieldTypeProfile(),
                indexSet.dataTiering(),
                indexSet.dataTiering() == null);

    }

    @JsonProperty("id")
    @Nullable
    public abstract String id();

    @JsonProperty("title")
    @NotBlank
    public abstract String title();

    @JsonProperty("description")
    @Nullable
    public abstract String description();

    @JsonProperty("default")
    public abstract boolean isDefault();

    @JsonProperty("can_be_default")
    public abstract boolean canBeDefault();

    @JsonProperty("writable")
    public abstract boolean isWritable();

    @JsonProperty("index_prefix")
    @Pattern(regexp = IndexSetConfig.INDEX_PREFIX_REGEX)
    @SizeInBytes(message = "Index prefix must have a length in bytes between {min} and {max}", min = 1, max = 250)
    public abstract String indexPrefix();

    @JsonProperty("shards")
    @Min(1)
    public abstract int shards();

    @JsonProperty("replicas")
    @Min(0)
    public abstract int replicas();

    @Nullable
    @JsonProperty(FIELD_ROTATION_STRATEGY_CLASS)
    public abstract String rotationStrategyClass();

    @Nullable
    @JsonProperty(FIELD_ROTATION_STRATEGY)
    public abstract RotationStrategyConfig rotationStrategy();

    @Nullable
    @JsonProperty(FIELD_RETENTION_STRATEGY_CLASS)
    public abstract String retentionStrategyClass();

    @Nullable
    @JsonProperty(FIELD_RETENTION_STRATEGY)
    public abstract RetentionStrategyConfig retentionStrategy();

    @JsonProperty("creation_date")
    @NotNull
    public abstract ZonedDateTime creationDate();

    @JsonProperty("index_analyzer")
    @NotBlank
    public abstract String indexAnalyzer();

    @JsonProperty("index_optimization_max_num_segments")
    @Min(1L)
    public abstract int indexOptimizationMaxNumSegments();

    @JsonProperty("index_optimization_disabled")
    public abstract boolean indexOptimizationDisabled();

    @JsonProperty("field_type_refresh_interval")
    public abstract Duration fieldTypeRefreshInterval();

    @JsonProperty("index_template_type")
    public abstract Optional<String> templateType();

    @JsonProperty(FIELD_PROFILE_ID)
    @Nullable
    public abstract String fieldTypeProfile();

    @Nullable
    @JsonProperty(FIELD_DATA_TIERING)
    public abstract DataTieringConfig dataTiering();

    @JsonProperty(FIELD_USE_LEGACY_ROTATION)
    public abstract Boolean useLegacyRotation();

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
                .rotationStrategy(rotationStrategy())
                .retentionStrategyClass(retentionStrategyClass())
                .retentionStrategy(retentionStrategy())
                .creationDate(creationDate())
                .indexAnalyzer(indexAnalyzer())
                .indexTemplateName(indexPrefix() + "-template")
                .indexOptimizationMaxNumSegments(indexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(indexOptimizationDisabled())
                .fieldTypeRefreshInterval(fieldTypeRefreshInterval())
                .fieldTypeProfile(fieldTypeProfile())
                .dataTiering(Boolean.FALSE.equals(useLegacyRotation()) ? dataTiering() : null);

        final IndexSetConfig.Builder builderWithTemplateType = templateType().map(builder::indexTemplateType).orElse(builder);
        return builderWithTemplateType.build();
    }
}
