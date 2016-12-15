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
package org.graylog2.rest.resources.system.indexer.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.hibernate.validator.constraints.NotBlank;

import javax.annotation.Nullable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@AutoValue
@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class IndexSetUpdateRequest {
    @JsonProperty("id")
    public abstract String id();

    @JsonProperty("title")
    @NotBlank
    public abstract String title();

    @JsonProperty("description")
    @Nullable
    public abstract String description();

    @JsonProperty("writable")
    public abstract boolean isWritable();

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

    @JsonProperty("index_analyzer")
    @NotBlank
    public abstract String indexAnalyzer();

    @JsonProperty("index_template_name")
    @NotBlank
    public abstract String indexTemplateName();

    @JsonProperty("index_optimization_max_num_segments")
    @Min(1L)
    public abstract int indexOptimizationMaxNumSegments();

    @JsonProperty("index_optimization_disabled")
    public abstract boolean indexOptimizationDisabled();

    @JsonCreator
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static IndexSetUpdateRequest create(@JsonProperty("id") String id,
                                               @JsonProperty("title") @NotBlank String title,
                                               @JsonProperty("description") @Nullable String description,
                                               @JsonProperty("writable") boolean isWritable,
                                               @JsonProperty("shards") @Min(1) int shards,
                                               @JsonProperty("replicas") @Min(0) int replicas,
                                               @JsonProperty("rotation_strategy_class") @NotNull String rotationStrategyClass,
                                               @JsonProperty("rotation_strategy") @NotNull RotationStrategyConfig rotationStrategy,
                                               @JsonProperty("retention_strategy_class") @NotNull String retentionStrategyClass,
                                               @JsonProperty("retention_strategy") @NotNull RetentionStrategyConfig retentionStrategy,
                                               @JsonProperty("index_analyzer") @NotBlank String indexAnalyzer,
                                               @JsonProperty("index_template_name") @NotBlank String indexTemplateName,
                                               @JsonProperty("index_optimization_max_num_segments") @Min(1L) int indexOptimizationMaxNumSegments,
                                               @JsonProperty("index_optimization_disabled") boolean indexOptimizationDisabled) {
        return new AutoValue_IndexSetUpdateRequest(id, title, description, isWritable, shards, replicas,
                rotationStrategyClass, rotationStrategy, retentionStrategyClass, retentionStrategy,
                indexAnalyzer, indexTemplateName, indexOptimizationMaxNumSegments, indexOptimizationDisabled);
    }

    public static IndexSetUpdateRequest fromIndexSetConfig(IndexSetConfig indexSet) {
        return create(
                indexSet.id(),
                indexSet.title(),
                indexSet.description(),
                indexSet.isWritable(),
                indexSet.shards(),
                indexSet.replicas(),
                indexSet.rotationStrategyClass(),
                indexSet.rotationStrategy(),
                indexSet.retentionStrategyClass(),
                indexSet.retentionStrategy(),
                indexSet.indexAnalyzer(),
                indexSet.indexTemplateName(),
                indexSet.indexOptimizationMaxNumSegments(),
                indexSet.indexOptimizationDisabled());

    }

    public IndexSetConfig toIndexSetConfig(IndexSetConfig oldConfig) {
        return IndexSetConfig.create(
                id(),
                title(),
                description(),
                isWritable(),
                oldConfig.indexPrefix(),
                oldConfig.indexMatchPattern(),
                oldConfig.indexWildcard(),
                shards(),
                replicas(),
                rotationStrategyClass(),
                rotationStrategy(),
                retentionStrategyClass(),
                retentionStrategy(),
                oldConfig.creationDate(),
                indexAnalyzer(),
                indexTemplateName(),
                indexOptimizationMaxNumSegments(),
                indexOptimizationDisabled());
    }
}
