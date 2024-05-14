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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ComparisonChain;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.database.DbEntity;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.indexer.MessageIndexTemplateProvider;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.validation.SizeInBytes;
import org.graylog2.validation.ValidObjectId;
import org.joda.time.Duration;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.indexer.EventIndexTemplateProvider.EVENT_TEMPLATE_TYPE;
import static org.graylog2.shared.security.RestPermissions.INDEXSETS_READ;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
@DbEntity(collection = MongoIndexSetService.COLLECTION_NAME,
          readPermission = INDEXSETS_READ)
public abstract class IndexSetConfig implements Comparable<IndexSetConfig>, SimpleIndexSetConfig {
    public static final String INDEX_PREFIX_REGEX = "^[a-z0-9][a-z0-9_+-]*$";
    public static final String DEFAULT_INDEX_TEMPLATE_TYPE = MessageIndexTemplateProvider.MESSAGE_TEMPLATE_TYPE;

    public static final String FIELD_REGULAR = "regular";
    public static final String FIELD_INDEX_PREFIX = "index_prefix";
    public static final String FIELD_INDEX_MATCH_PATTERN = "index_match_pattern";
    public static final String FIELD_INDEX_WILDCARD = "index_wildcard";
    public static final String FIELD_CREATION_DATE = "creation_date";
    public static final String FIELD_INDEX_TEMPLATE_NAME = "index_template_name";
    public static final String FIELD_INDEX_TEMPLATE_TYPE = "index_template_type";
    public static final String FIELD_TYPE_REFRESH_INTERVAL = "field_type_refresh_interval";
    public static final String FIELD_CUSTOM_FIELD_MAPPINGS = "custom_field_mappings";
    public static final String FIELD_PROFILE_ID = "field_type_profile";

    @JsonCreator
    public static IndexSetConfig create(@Id @ObjectId @JsonProperty("_id") @Nullable String id,
                                        @JsonProperty(FIELD_TITLE) @NotBlank String title,
                                        @JsonProperty(FIELD_DESCRIPTION) @Nullable String description,
                                        @JsonProperty(FIELD_WRITABLE) @Nullable Boolean isWritable,
                                        @JsonProperty(FIELD_REGULAR) @Nullable Boolean isRegular,
                                        @JsonProperty(FIELD_INDEX_PREFIX) @Pattern(regexp = INDEX_PREFIX_REGEX) String indexPrefix,
                                        @JsonProperty(FIELD_INDEX_MATCH_PATTERN) @Nullable String indexMatchPattern,
                                        @JsonProperty(FIELD_INDEX_WILDCARD) @Nullable String indexWildcard,
                                        @JsonProperty(FIELD_SHARDS) @Min(1) int shards,
                                        @JsonProperty(FIELD_REPLICAS) @Min(0) int replicas,
                                        @JsonProperty(FIELD_ROTATION_STRATEGY_CLASS) @Nullable String rotationStrategyClass,
                                        @JsonProperty(FIELD_ROTATION_STRATEGY) @Nullable RotationStrategyConfig rotationStrategy,
                                        @JsonProperty(FIELD_RETENTION_STRATEGY_CLASS) @Nullable String retentionStrategyClass,
                                        @JsonProperty(FIELD_RETENTION_STRATEGY) @Nullable RetentionStrategyConfig retentionStrategy,
                                        @JsonProperty(FIELD_CREATION_DATE) @NotNull ZonedDateTime creationDate,
                                        @JsonProperty(FIELD_INDEX_ANALYZER) @Nullable String indexAnalyzer,
                                        @JsonProperty(FIELD_INDEX_TEMPLATE_NAME) @Nullable String indexTemplateName,
                                        @JsonProperty(FIELD_INDEX_TEMPLATE_TYPE) @Nullable String indexTemplateType,
                                        @JsonProperty(FIELD_INDEX_OPTIMIZATION_MAX_NUM_SEGMENTS) @Nullable Integer maxNumSegments,
                                        @JsonProperty(FIELD_INDEX_OPTIMIZATION_DISABLED) @Nullable Boolean indexOptimizationDisabled,
                                        @JsonProperty(FIELD_TYPE_REFRESH_INTERVAL) @Nullable Duration fieldTypeRefreshInterval,
                                        @JsonProperty(FIELD_CUSTOM_FIELD_MAPPINGS) @Nullable CustomFieldMappings customFieldMappings,
                                        @JsonProperty(FIELD_PROFILE_ID) @ValidObjectId @Nullable String fieldTypeProfile,
                                        @JsonProperty(FIELD_DATA_TIERING) @Nullable DataTieringConfig dataTiering
    ) {

        final boolean writableValue = isWritable == null || isWritable;

        Duration fieldTypeRefreshIntervalValue = fieldTypeRefreshInterval;
        if (fieldTypeRefreshIntervalValue == null) {
            // No need to periodically refresh the field types for a non-writable index set
            fieldTypeRefreshIntervalValue = writableValue ? DEFAULT_FIELD_TYPE_REFRESH_INTERVAL : Duration.ZERO;
        }

        return AutoValue_IndexSetConfig.builder()
                .id(id)
                .title(title)
                .description(description)
                .isWritable(writableValue)
                .isRegular(isRegular)
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
                .indexTemplateType(indexTemplateType)
                .indexOptimizationMaxNumSegments(maxNumSegments == null ? 1 : maxNumSegments)
                .indexOptimizationDisabled(indexOptimizationDisabled != null && indexOptimizationDisabled)
                .fieldTypeRefreshInterval(fieldTypeRefreshIntervalValue)
                .customFieldMappings(customFieldMappings == null ? new CustomFieldMappings() : customFieldMappings)
                .fieldTypeProfile(fieldTypeProfile)
                .dataTiering(dataTiering)
                .build();
    }

    // Compatibility creator after field type refresh interval has been introduced
    public static IndexSetConfig create(String id,
                                        String title,
                                        String description,
                                        boolean isWritable,
                                        Boolean isRegular,
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
                                        String indexTemplateType,
                                        int indexOptimizationMaxNumSegments,
                                        boolean indexOptimizationDisabled) {
        return create(id, title, description, isWritable, isRegular, indexPrefix, null, null, shards, replicas,
                rotationStrategyClass, rotationStrategy, retentionStrategyClass, retentionStrategy, creationDate,
                indexAnalyzer, indexTemplateName, indexTemplateType, indexOptimizationMaxNumSegments, indexOptimizationDisabled,
                DEFAULT_FIELD_TYPE_REFRESH_INTERVAL, new CustomFieldMappings(), null, null);
    }

    // Compatibility creator after field type refresh interval has been introduced
    public static IndexSetConfig create(String title,
                                        String description,
                                        boolean isWritable,
                                        Boolean isRegular,
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
                                        String indexTemplateType,
                                        int indexOptimizationMaxNumSegments,
                                        boolean indexOptimizationDisabled) {
        return create(null, title, description, isWritable, isRegular, indexPrefix, null, null, shards, replicas,
                rotationStrategyClass, rotationStrategy, retentionStrategyClass, retentionStrategy, creationDate,
                indexAnalyzer, indexTemplateName, indexTemplateType, indexOptimizationMaxNumSegments, indexOptimizationDisabled,
                DEFAULT_FIELD_TYPE_REFRESH_INTERVAL, new CustomFieldMappings(), null, null);
    }

    /**
     * For non-UI originating instances, use {@link IndexSetConfigFactory} instead to create an instance with
     * appropriate defaults.
     */
    public static Builder builder() {
        return new AutoValue_IndexSetConfig.Builder()
                // Index sets are writable by default.
                .isWritable(true)
                .customFieldMappings(new CustomFieldMappings())
                .fieldTypeRefreshInterval(DEFAULT_FIELD_TYPE_REFRESH_INTERVAL);
    }

    @JsonProperty("id")
    @Nullable
    @Id
    @ObjectId
    public abstract String id();

    @JsonProperty(FIELD_TITLE)
    @NotBlank
    public abstract String title();

    @JsonProperty(FIELD_DESCRIPTION)
    @Nullable
    public abstract String description();

    @JsonProperty(FIELD_WRITABLE)
    public abstract boolean isWritable();

    /**
     * Indicates whether this index set is intended to
     * store messages ingested by user, not by the system
     */
    @JsonProperty(FIELD_REGULAR)
    public abstract Optional<Boolean> isRegular();

    @JsonProperty(FIELD_INDEX_PREFIX)
    @NotBlank
    @Pattern(regexp = INDEX_PREFIX_REGEX)
    @SizeInBytes(message = "Index prefix must have a length in bytes between {min} and {max}", min = 1, max = 250)
    public abstract String indexPrefix();

    @JsonProperty(FIELD_INDEX_MATCH_PATTERN)
    @Nullable
    public abstract String indexMatchPattern();

    @JsonProperty(FIELD_INDEX_WILDCARD)
    @Nullable
    public abstract String indexWildcard();

    @JsonProperty(FIELD_CREATION_DATE)
    @NotNull
    public abstract ZonedDateTime creationDate();

    @JsonProperty(FIELD_INDEX_ANALYZER)
    @NotBlank
    public abstract String indexAnalyzer();

    @JsonProperty(FIELD_INDEX_TEMPLATE_NAME)
    @NotBlank
    public abstract String indexTemplateName();

    @JsonProperty(FIELD_INDEX_TEMPLATE_TYPE)
    @NotBlank
    public abstract Optional<String> indexTemplateType();

    @JsonProperty(FIELD_CUSTOM_FIELD_MAPPINGS)
    public abstract CustomFieldMappings customFieldMappings();

    @JsonProperty(FIELD_PROFILE_ID)
    @Nullable
    public abstract String fieldTypeProfile();

    @JsonIgnore
    public boolean isRegularIndex() {
        final String indexTemplate = indexTemplateType().orElse(null);
        return isWritable() && (indexTemplate == null || DEFAULT_INDEX_TEMPLATE_TYPE.equals(indexTemplate) ||
                isRegular().orElse(false));
    }

    @JsonIgnore
    public boolean canHaveCustomFieldMappings() {
        final String indexTemplateType = this.indexTemplateType().orElse(null);
        if (EVENT_TEMPLATE_TYPE.equals(indexTemplateType) || "failures".equals(indexTemplateType)) {
            return false;
        }
        return true;
    }

    @JsonIgnore
    public boolean canHaveProfile() {
        final String indexTemplateType = this.indexTemplateType().orElse(null);
        if (EVENT_TEMPLATE_TYPE.equals(indexTemplateType) || "failures".equals(indexTemplateType)) {
            return false;
        }
        return true;
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

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder description(String description);

        public abstract Builder isWritable(boolean isWritable);

        public abstract Builder isRegular(@Nullable Boolean isRegular);

        public abstract Builder indexPrefix(String indexPrefix);

        public abstract Builder indexMatchPattern(String indexMatchPattern);

        public abstract Builder indexWildcard(String indexWildcard);

        public abstract Builder shards(int shards);

        public abstract Builder replicas(int replicas);

        public abstract Builder rotationStrategyClass(@Nullable String rotationStrategyClass);

        public abstract Builder rotationStrategy(@Nullable RotationStrategyConfig rotationStrategy);

        public abstract Builder retentionStrategyClass(@Nullable String retentionStrategyClass);

        public abstract Builder retentionStrategy(@Nullable RetentionStrategyConfig retentionStrategy);

        public abstract Builder creationDate(ZonedDateTime creationDate);

        public abstract Builder indexAnalyzer(String analyzer);

        public abstract Builder indexTemplateName(String templateName);

        public abstract Builder indexTemplateType(@Nullable String templateType);

        public abstract Builder indexOptimizationMaxNumSegments(int indexOptimizationMaxNumSegments);

        public abstract Builder indexOptimizationDisabled(boolean indexOptimizationDisabled);

        public abstract Builder fieldTypeRefreshInterval(Duration fieldTypeRefreshInterval);

        public abstract Builder customFieldMappings(CustomFieldMappings customFieldMappings);

        public abstract Builder fieldTypeProfile(String fieldTypeProfile);

        public abstract Builder dataTiering(@Nullable DataTieringConfig dataTiering);

        public abstract IndexSetConfig build();
    }
}
