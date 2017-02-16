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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ComparisonChain;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.hibernate.validator.constraints.NotBlank;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.ZonedDateTime;

import static com.google.common.base.Strings.isNullOrEmpty;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
// Ignore deprecated "default" message field. Only relevant for Graylog 2.2.0-beta.[12] users.
// TODO: Remove in Graylog 3.0.0
@JsonIgnoreProperties({"default"})
public abstract class IndexSetConfig implements Comparable<IndexSetConfig> {
    public static final String FIELD_INDEX_PREFIX = "index_prefix";
    public static final String FIELD_CREATION_DATE = "creation_date";
    public static final String INDEX_PREFIX_REGEX = "^[a-z0-9][a-z0-9_+-]*$";

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
    @Pattern(regexp = INDEX_PREFIX_REGEX)
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
    public static IndexSetConfig create(@Id @ObjectId @JsonProperty("_id") @Nullable String id,
                                        @JsonProperty("title") @NotBlank String title,
                                        @JsonProperty("description") @Nullable String description,
                                        @JsonProperty("writable") @Nullable Boolean isWritable,
                                        @JsonProperty(FIELD_INDEX_PREFIX) @Pattern(regexp = INDEX_PREFIX_REGEX) String indexPrefix,
                                        @JsonProperty("index_match_pattern") @Nullable String indexMatchPattern,
                                        @JsonProperty("index_wildcard") @Nullable String indexWildcard,
                                        @JsonProperty("shards") @Min(1) int shards,
                                        @JsonProperty("replicas") @Min(0) int replicas,
                                        @JsonProperty("rotation_strategy_class") @Nullable String rotationStrategyClass,
                                        @JsonProperty("rotation_strategy") @NotNull RotationStrategyConfig rotationStrategy,
                                        @JsonProperty("retention_strategy_class") @Nullable String retentionStrategyClass,
                                        @JsonProperty("retention_strategy") @NotNull RetentionStrategyConfig retentionStrategy,
                                        @JsonProperty(FIELD_CREATION_DATE) @NotNull ZonedDateTime creationDate,
                                        @JsonProperty("index_analyzer") @Nullable String indexAnalyzer,
                                        @JsonProperty("index_template_name") @Nullable String indexTemplateName,
                                        @JsonProperty("index_optimization_max_num_segments") @Nullable Integer maxNumSegments,
                                        @JsonProperty("index_optimization_disabled") @Nullable Boolean indexOptimizationDisabled) {
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
                .indexAnalyzer(isNullOrEmpty(indexAnalyzer) ? "standard" : indexAnalyzer)
                .indexTemplateName(isNullOrEmpty(indexTemplateName) ? indexPrefix + "-template" : indexTemplateName)
                .indexOptimizationMaxNumSegments(maxNumSegments == null ? 1 : maxNumSegments)
                .indexOptimizationDisabled(indexOptimizationDisabled == null ? false : indexOptimizationDisabled)
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
                                        ZonedDateTime creationDate,
                                        String indexAnalyzer,
                                        String indexTemplateName,
                                        int indexOptimizationMaxNumSegments,
                                        boolean indexOptimizationDisabled) {
        return create(id, title, description, isWritable, indexPrefix, null, null, shards, replicas,
                rotationStrategyClass, rotationStrategy, retentionStrategyClass, retentionStrategy, creationDate,
                indexAnalyzer, indexTemplateName, indexOptimizationMaxNumSegments, indexOptimizationDisabled);
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
                                        ZonedDateTime creationDate,
                                        String indexAnalyzer,
                                        String indexTemplateName,
                                        int indexOptimizationMaxNumSegments,
                                        boolean indexOptimizationDisabled) {
        return create(null, title, description, isWritable, indexPrefix, null, null, shards, replicas,
                rotationStrategyClass, rotationStrategy, retentionStrategyClass, retentionStrategy, creationDate,
                indexAnalyzer, indexTemplateName, indexOptimizationMaxNumSegments, indexOptimizationDisabled);
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

        public abstract Builder indexAnalyzer(String analyzer);

        public abstract Builder indexTemplateName(String templateName);

        public abstract Builder indexOptimizationMaxNumSegments(int indexOptimizationMaxNumSegments);

        public abstract Builder indexOptimizationDisabled(boolean indexOptimizationDisabled);

        public abstract IndexSetConfig build();
    }
}
