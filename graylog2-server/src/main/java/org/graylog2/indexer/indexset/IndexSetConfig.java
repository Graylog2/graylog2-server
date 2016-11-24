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
package org.graylog2.indexer.indexset;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ComparisonChain;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.hibernate.validator.constraints.NotBlank;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;

@AutoValue
@JsonAutoDetect
public abstract class IndexSetConfig implements Comparable<IndexSetConfig> {
    @JsonProperty("id")
    @Nullable
    @Id
    @ObjectId
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

    @JsonProperty("rotation_strategy_class")
    @Nullable
    public abstract String rotationStrategyClass();

    @JsonProperty("rotation_strategy")
    @NotNull
    public abstract RotationStrategyConfig rotationStrategy();

    @JsonProperty("retention_strategy_class")
    @Nullable
    public abstract String retentionStrategyClass();

    @JsonProperty("retention_strategy")
    @NotNull
    public abstract RetentionStrategyConfig retentionStrategy();

    @JsonProperty("creation_date")
    @NotNull
    public abstract ZonedDateTime creationDate();

    // TODO 2.2: Migrate config setting to index set!
    @JsonProperty("index_analyzer")
    @JsonIgnore
    public String indexAnalyzer() {
        return "standard";
    }

    // TODO 2.2: Migrate config setting to index set!
    @JsonProperty("index_template_name")
    @JsonIgnore
    public String indexTemplateName() {
        return "graylog-internal";
    }

    // TODO 2.2: Migrate config setting to index set!
    @JsonProperty("index_optimization_max_num_segments")
    @JsonIgnore
    public int indexOptimizationMaxNumSegments() {
        return 1;
    }

    // TODO 2.2: Migrate config setting to index set!
    @JsonProperty("index_optimization_disabled")
    @JsonIgnore
    public boolean indexOptimizationDisabled() {
        return false;
    }

    @JsonCreator
    public static IndexSetConfig create(@Id @ObjectId @JsonProperty("_id") @Nullable String id,
                                        @JsonProperty("title") @NotBlank String title,
                                        @JsonProperty("description") @Nullable String description,
                                        @JsonProperty("index_prefix") @NotBlank String indexPrefix,
                                        @JsonProperty("shards") @Min(1) int shards,
                                        @JsonProperty("replicas") @Min(0) int replicas,
                                        @JsonProperty("rotation_strategy_class") @Nullable String rotationStrategyClass,
                                        @JsonProperty("rotation_strategy") @NotNull RotationStrategyConfig rotationStrategy,
                                        @JsonProperty("retention_strategy_class") @Nullable String retentionStrategyClass,
                                        @JsonProperty("retention_strategy") @NotNull RetentionStrategyConfig retentionStrategy,
                                        @JsonProperty("creation_date") @NotNull ZonedDateTime creationDate) {
        return AutoValue_IndexSetConfig.builder()
                .id(id)
                .title(title)
                .description(description)
                .indexPrefix(indexPrefix)
                .shards(shards)
                .replicas(replicas)
                .rotationStrategyClass(rotationStrategyClass)
                .rotationStrategy(rotationStrategy)
                .retentionStrategyClass(retentionStrategyClass)
                .retentionStrategy(retentionStrategy)
                .creationDate(creationDate)
                .build();
    }

    public static IndexSetConfig create(String title,
                                        String description,
                                        String indexPrefix,
                                        int shards,
                                        int replicas,
                                        String rotationStrategyClass,
                                        RotationStrategyConfig rotationStrategy,
                                        String retentionStrategyClass,
                                        RetentionStrategyConfig retentionStrategy,
                                        ZonedDateTime creationDate) {
        return create(null, title, description, indexPrefix, shards, replicas, rotationStrategyClass, rotationStrategy, retentionStrategyClass, retentionStrategy, creationDate);
    }

    @Override
    public int compareTo(IndexSetConfig o) {
        return ComparisonChain.start()
                .compare(title(), o.title())
                .compare(indexPrefix(), o.indexPrefix())
                .compare(creationDate(), o.creationDate())
                .result();
    }

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_IndexSetConfig.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder description(String description);

        public abstract Builder indexPrefix(String indexPrefix);

        public abstract Builder shards(int shards);

        public abstract Builder replicas(int replicas);

        public abstract Builder rotationStrategyClass(String rotationStrategyClass);

        public abstract Builder rotationStrategy(RotationStrategyConfig rotationStrategy);

        public abstract Builder retentionStrategyClass(String retentionStrategyClass);

        public abstract Builder retentionStrategy(RetentionStrategyConfig retentionStrategy);

        public abstract Builder creationDate(ZonedDateTime creationDate);

        public abstract IndexSetConfig build();
    }
}
