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
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.joda.time.Duration;

import javax.annotation.Nullable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.ZonedDateTime;
import java.util.Optional;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class IndexSetSummary {
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

    @JsonProperty("writable")
    public abstract boolean isWritable();

    @JsonProperty("index_prefix")
    @Pattern(regexp = IndexSetConfig.INDEX_PREFIX_REGEX)
    public abstract String indexPrefix();

    @JsonProperty("shards")
    @Min(1)
    public abstract int shards();

    @JsonProperty("replicas")
    @Min(0)
    public abstract int replicas();

    @JsonProperty("rotation_strategy_class")
    @NotNull
    public abstract String rotationStrategyClass();

    @JsonProperty("rotation_strategy")
    @NotNull
    public abstract RotationStrategyConfig rotationStrategy();

    @JsonProperty("retention_strategy_class")
    @NotNull
    public abstract String retentionStrategyClass();

    @JsonProperty("retention_strategy")
    @NotNull
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
    public abstract Optional<IndexSetConfig.TemplateType> templateType();

    @JsonCreator
    public static IndexSetSummary create(@JsonProperty("id") @Nullable String id,
                                         @JsonProperty("title") @NotBlank String title,
                                         @JsonProperty("description") @Nullable String description,
                                         @JsonProperty("default") boolean isDefault,
                                         @JsonProperty("writable") boolean isWritable,
                                         @JsonProperty("index_prefix") @Pattern(regexp = IndexSetConfig.INDEX_PREFIX_REGEX) String indexPrefix,
                                         @JsonProperty("shards") @Min(1) int shards,
                                         @JsonProperty("replicas") @Min(0) int replicas,
                                         @JsonProperty("rotation_strategy_class") @NotNull String rotationStrategyClass,
                                         @JsonProperty("rotation_strategy") @NotNull RotationStrategyConfig rotationStrategy,
                                         @JsonProperty("retention_strategy_class") @NotNull String retentionStrategyClass,
                                         @JsonProperty("retention_strategy") @NotNull RetentionStrategyConfig retentionStrategy,
                                         @JsonProperty("creation_date") @NotNull ZonedDateTime creationDate,
                                         @JsonProperty("index_analyzer") @NotBlank String indexAnalyzer,
                                         @JsonProperty("index_optimization_max_num_segments") @Min(1L) int indexOptimizationMaxNumSegments,
                                         @JsonProperty("index_optimization_disabled") boolean indexOptimizationDisabled,
                                         @JsonProperty("field_type_refresh_interval") Duration fieldTypeRefreshInterval,
                                         @JsonProperty("index_template_type") @Nullable IndexSetConfig.TemplateType templateType) {
        return new AutoValue_IndexSetSummary(id, title, description, isDefault, isWritable, indexPrefix, shards, replicas,
                rotationStrategyClass, rotationStrategy, retentionStrategyClass, retentionStrategy, creationDate,
                indexAnalyzer, indexOptimizationMaxNumSegments, indexOptimizationDisabled, fieldTypeRefreshInterval,
                Optional.ofNullable(templateType));
    }

    public static IndexSetSummary fromIndexSetConfig(IndexSetConfig indexSet, boolean isDefault) {
        return create(
                indexSet.id(),
                indexSet.title(),
                indexSet.description(),
                isDefault,
                indexSet.isWritable(),
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
                indexSet.indexTemplateType().orElse(null));

    }

    public IndexSetConfig toIndexSetConfig() {
        final IndexSetConfig.Builder builder = IndexSetConfig.builder()
                .id(id())
                .title(title())
                .description(description())
                .isWritable(isWritable())
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
                .fieldTypeRefreshInterval(fieldTypeRefreshInterval());

        final IndexSetConfig.Builder builderWithTemplateType = templateType().map(builder::indexTemplateType).orElse(builder);
        return builderWithTemplateType.build();
    }
}
