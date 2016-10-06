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
package org.graylog2.rest.resources.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.hibernate.validator.constraints.NotBlank;

import javax.annotation.Nullable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;

@AutoValue
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

    @JsonProperty("index_prefix")
    @NotBlank
    public abstract String indexPrefix();

    @JsonProperty("shards")
    @Min(1)
    public abstract int shards();

    @JsonProperty("replicas")
    @Min(0)
    public abstract int replicas();

    @JsonProperty("rotation_strategy")
    @NotNull
    public abstract RotationStrategyConfig rotationStrategy();

    @JsonProperty("retention_strategy")
    @NotNull
    public abstract RetentionStrategyConfig retentionStrategy();

    @JsonProperty("creation_date")
    @NotNull
    public abstract ZonedDateTime creationDate();

    @JsonCreator
    public static IndexSetSummary create(@JsonProperty("id") @Nullable String id,
                                         @JsonProperty("title") @NotBlank String title,
                                         @JsonProperty("description") @Nullable String description,
                                         @JsonProperty("index_prefix") @NotBlank String indexPrefix,
                                         @JsonProperty("shards") @Min(1) int shards,
                                         @JsonProperty("replicas") @Min(0) int replicas,
                                         @JsonProperty("rotation_strategy") @NotNull RotationStrategyConfig rotationStrategy,
                                         @JsonProperty("retention_strategy") @NotNull RetentionStrategyConfig retentionStrategy,
                                         @JsonProperty("creation_date") @NotNull ZonedDateTime creationDate) {
        return new AutoValue_IndexSetSummary(id, title, description, indexPrefix, shards, replicas, rotationStrategy, retentionStrategy, creationDate);
    }

    public static IndexSetSummary fromIndexSetConfig(IndexSetConfig indexSet) {
        return create(
                indexSet.id(),
                indexSet.title(),
                indexSet.description(),
                indexSet.indexPrefix(),
                indexSet.shards(),
                indexSet.replicas(),
                indexSet.rotationStrategy(),
                indexSet.retentionStrategy(),
                indexSet.creationDate());

    }

    public IndexSetConfig toIndexSetConfig() {
        return IndexSetConfig.create(
                id(),
                title(),
                description(),
                indexPrefix(),
                shards(),
                replicas(),
                rotationStrategy(),
                retentionStrategy(),
                creationDate());
    }
}
