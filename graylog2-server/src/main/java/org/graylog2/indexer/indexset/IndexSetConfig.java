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
import org.graylog.autovalue.WithBeanGetter;
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

import static java.util.Objects.requireNonNull;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class IndexSetConfig implements Comparable<IndexSetConfig> {
    public static final String FIELD_INDEX_PREFIX = "index_prefix";
    public static final String FIELD_CREATION_DATE = "creation_date";

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

    @JsonProperty("writable")
    public abstract boolean isWritable();

    @JsonProperty(FIELD_INDEX_PREFIX)
    @NotBlank
    public abstract String indexPrefix();

    @JsonProperty("index_match_pattern")
    @Nullable
    public abstract String indexMatchPattern();

    @JsonProperty("index_wildcard")
    @Nullable
    public abstract String indexWildcard();

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

    @JsonProperty(FIELD_CREATION_DATE)
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
        return "graylog-internal-" + requireNonNull(id());
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

    // TODO 2.2: creation_date is a string but needs to be a date - look at AlertImpl for example
    @JsonCreator
    public static IndexSetConfig create(@Id @ObjectId @JsonProperty("_id") @Nullable String id,
                                        @JsonProperty("title") @NotBlank String title,
                                        @JsonProperty("description") @Nullable String description,
                                        // TODO 3.0: Remove "default" field here. A migration in 2.2 removed it.
                                        @JsonProperty("default") @Nullable Boolean isDefault, // Ignored, older objects might still have it
                                        @JsonProperty("writable") @Nullable Boolean isWritable,
                                        @JsonProperty(FIELD_INDEX_PREFIX) @NotBlank String indexPrefix,
                                        @JsonProperty("index_match_pattern") @Nullable String indexMatchPattern,
                                        @JsonProperty("index_wildcard") @Nullable String indexWildcard,
                                        @JsonProperty("shards") @Min(1) int shards,
                                        @JsonProperty("replicas") @Min(0) int replicas,
                                        @JsonProperty("rotation_strategy_class") @Nullable String rotationStrategyClass,
                                        @JsonProperty("rotation_strategy") @NotNull RotationStrategyConfig rotationStrategy,
                                        @JsonProperty("retention_strategy_class") @Nullable String retentionStrategyClass,
                                        @JsonProperty("retention_strategy") @NotNull RetentionStrategyConfig retentionStrategy,
                                        @JsonProperty(FIELD_CREATION_DATE) @NotNull ZonedDateTime creationDate) {
        return AutoValue_IndexSetConfig.builder()
                .id(id)
                .title(title)
                .description(description)
                .isWritable(isWritable == null ? true : isWritable)
                .indexPrefix(indexPrefix)
                .indexMatchPattern(indexMatchPattern)
                .indexWildcard(indexWildcard)
                .shards(shards)
                .replicas(replicas)
                .rotationStrategyClass(rotationStrategyClass)
                .rotationStrategy(rotationStrategy)
                .retentionStrategyClass(retentionStrategyClass)
                .retentionStrategy(retentionStrategy)
                .creationDate(creationDate)
                .build();
    }

    public static IndexSetConfig create(String id,
                                        String title,
                                        String description,
                                        boolean isWritable,
                                        String indexPrefix,
                                        int shards,
                                        int replicas,
                                        String rotationStrategyClass,
                                        RotationStrategyConfig rotationStrategy,
                                        String retentionStrategyClass,
                                        RetentionStrategyConfig retentionStrategy,
                                        ZonedDateTime creationDate) {
        return create(id, title, description, null, isWritable, indexPrefix, null, null, shards, replicas, rotationStrategyClass, rotationStrategy, retentionStrategyClass, retentionStrategy, creationDate);
    }

    public static IndexSetConfig create(String title,
                                        String description,
                                        boolean isWritable,
                                        String indexPrefix,
                                        int shards,
                                        int replicas,
                                        String rotationStrategyClass,
                                        RotationStrategyConfig rotationStrategy,
                                        String retentionStrategyClass,
                                        RetentionStrategyConfig retentionStrategy,
                                        ZonedDateTime creationDate) {
        return create(null, title, description, null, isWritable, indexPrefix, null, null, shards, replicas, rotationStrategyClass, rotationStrategy, retentionStrategyClass, retentionStrategy, creationDate);
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
        // Index sets are writable by default.
        return new AutoValue_IndexSetConfig.Builder().isWritable(true);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder description(String description);

        public abstract Builder isWritable(boolean isWritable);

        public abstract Builder indexPrefix(String indexPrefix);

        public abstract Builder indexMatchPattern(String indexMatchPattern);

        public abstract Builder indexWildcard(String indexWildcard);

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
