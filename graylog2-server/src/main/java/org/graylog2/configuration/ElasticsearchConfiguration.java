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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.converters.StringListConverter;
import com.github.joschi.jadconfig.converters.StringSetConverter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.util.Size;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.PositiveLongValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import org.graylog2.configuration.validators.RetentionStrategyValidator;
import org.graylog2.configuration.validators.RotationStrategyValidator;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategy;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategy;
import org.graylog2.indexer.rotation.tso.IndexLifetimeConfig;
import org.joda.time.Period;
import org.joda.time.Seconds;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.graylog2.shared.utilities.StringUtils.f;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class ElasticsearchConfiguration {
    public static final String MAX_INDEX_RETENTION_PERIOD = "max_index_retention_period";
    public static final String DEFAULT_EVENTS_INDEX_PREFIX = "default_events_index_prefix";
    public static final String DEFAULT_SYSTEM_EVENTS_INDEX_PREFIX = "default_system_events_index_prefix";
    public static final String TIME_SIZE_OPTIMIZING_RETENTION_MIN_LIFETIME = "time_size_optimizing_retention_min_lifetime";
    public static final String TIME_SIZE_OPTIMIZING_RETENTION_MAX_LIFETIME = "time_size_optimizing_retention_max_lifetime";
    public static final String TIME_SIZE_OPTIMIZING_RETENTION_FIXED_LEEWAY = "time_size_optimizing_retention_fixed_leeway";
    public static final String TIME_SIZE_OPTIMIZING_ROTATION_MIN_SHARD_SIZE = "time_size_optimizing_rotation_min_shard_size";
    public static final String TIME_SIZE_OPTIMIZING_ROTATION_MAX_SHARD_SIZE = "time_size_optimizing_rotation_max_shard_size";
    public static final String TIME_SIZE_OPTIMIZING_ROTATION_PERIOD = "time_size_optimizing_rotation_period";
    public static final String ALLOW_FLEXIBLE_RETENTION_PERIOD = "allow_flexible_retention_period";

    @Parameter(value = "elasticsearch_index_prefix", required = true)
    private String defaultIndexPrefix = "graylog";

    @Parameter(value = "elasticsearch_template_name")
    private String defaultIndexTemplateName = "graylog-internal";

    @Parameter(value = DEFAULT_EVENTS_INDEX_PREFIX, validators = StringNotBlankValidator.class)
    private String defaultEventsIndexPrefix = "gl-events";

    @Parameter(value = DEFAULT_SYSTEM_EVENTS_INDEX_PREFIX, validators = StringNotBlankValidator.class)
    private String defaultSystemEventsIndexPrefix = "gl-system-events";

    @Parameter(value = "elasticsearch_analyzer", required = true)
    private String analyzer = "standard";

    @Parameter(value = "elasticsearch_shards", validators = PositiveIntegerValidator.class, required = true)
    private int shards = 1;

    @Parameter(value = "elasticsearch_replicas", validators = PositiveIntegerValidator.class, required = true)
    private int replicas = 0;

    @Parameter(value = "disable_index_optimization")
    private boolean disableIndexOptimization = false;

    @Parameter(value = "index_optimization_max_num_segments", validators = PositiveIntegerValidator.class)
    private int indexOptimizationMaxNumSegments = 1;

    @Parameter(value = "index_field_type_periodical_full_refresh_interval", validators = {PositiveDurationValidator.class})
    private Duration indexFieldTypePeriodicalFullRefreshInterval = Duration.minutes(5);

    @Parameter(value = "retention_strategy", required = true)
    private String retentionStrategy = DeletionRetentionStrategy.NAME;

    @Parameter(value = "disabled_retention_strategies", required = true, converter = StringSetConverter.class, validators = {RetentionStrategyValidator.class})
    private Set<String> disabledRetentionStrategies = Collections.emptySet();

    @Parameter(value = "rotation_strategy", required = true)
    private String rotationStrategy = TimeBasedSizeOptimizingStrategy.NAME;

    // Rotation
    @Parameter(value = "elasticsearch_max_time_per_index", required = true)
    private Period maxTimePerIndex = Period.days(1);

    // Rotation
    @Parameter(value = "elasticsearch_rotate_empty_index_set", required = true)
    private boolean rotateEmptyIndex = false;

    // Rotation
    @Parameter(value = "elasticsearch_max_docs_per_index", validators = PositiveIntegerValidator.class, required = true)
    private int maxDocsPerIndex = 20000000;

    // Rotation
    @Parameter(value = "elasticsearch_max_size_per_index", validators = PositiveLongValidator.class, required = true)
    private long maxSizePerIndex = 30L * 1024 * 1024 * 1024; // 30GB

    // Retention
    @Parameter(value = "elasticsearch_max_number_of_indices", required = true, validators = PositiveIntegerValidator.class)
    private int maxNumberOfIndices = 20;

    // TimeBasedSizeOptimizingStrategy Rotation
    @Parameter(value = TIME_SIZE_OPTIMIZING_ROTATION_PERIOD)
    private Period timeSizeOptimizingRotationPeriod = Period.days(1);

    @Parameter(value = TIME_SIZE_OPTIMIZING_ROTATION_MIN_SHARD_SIZE)
    private Size timeSizeOptimizingRotationMinShardSize = Size.gigabytes(20);

    @Parameter(value = TIME_SIZE_OPTIMIZING_ROTATION_MAX_SHARD_SIZE)
    private Size timeSizeOptimizingRotationMaxShardSize = Size.gigabytes(50);

    @Parameter(value = TIME_SIZE_OPTIMIZING_RETENTION_MIN_LIFETIME)
    private Period timeSizeOptimizingRetentionMinLifeTime = IndexLifetimeConfig.DEFAULT_LIFETIME_MIN;

    @Parameter(value = TIME_SIZE_OPTIMIZING_RETENTION_MAX_LIFETIME)
    private Period timeSizeOptimizingRetentionMaxLifeTime = IndexLifetimeConfig.DEFAULT_LIFETIME_MAX;

    @Parameter(value = TIME_SIZE_OPTIMIZING_RETENTION_FIXED_LEEWAY)
    private Period timeSizeOptimizingRetentionFixedLeeway;

    @Parameter(value = ALLOW_FLEXIBLE_RETENTION_PERIOD)
    private boolean allowFlexibleRetentionPeriod = false;

    @Parameter(value = "elasticsearch_disable_version_check")
    private boolean disableVersionCheck = false;

    @Parameter(value = "elasticsearch_max_write_index_age")
    private Period maxWriteIndexAge = null;

    @Parameter(value = "no_retention")
    private boolean noRetention = false;

    @Parameter(value = "enabled_index_rotation_strategies", converter = StringListConverter.class, validators = RotationStrategyValidator.class)
    private List<String> enabledRotationStrategies = Arrays.asList(
            TimeBasedRotationStrategy.NAME, MessageCountRotationStrategy.NAME,
            SizeBasedRotationStrategy.NAME, TimeBasedSizeOptimizingStrategy.NAME);

    /**
     * Provides a hard upper limit for the retention period of any index set at configuration time.
     * <p>
     * This setting is used to validate the value a user chooses for {@code max_number_of_indices} when configuring
     * an index set. However, it is only in effect, when a <em>time-based rotation strategy</em> is chosen, because
     * otherwise it would be hard to calculate the effective retention period at configuration time.
     * <p>
     * If a rotation strategy other than time-based is selected and/or no value is provided for this setting, no upper
     * limit for index retention will be enforced.
     */
    @Parameter(value = MAX_INDEX_RETENTION_PERIOD)
    private Period maxIndexRetentionPeriod = null;
    @Parameter(value = "elasticsearch_index_optimization_timeout", validators = DurationCastedToIntegerValidator.class)
    private Duration indexOptimizationTimeout = Duration.hours(1L);
    @Parameter(value = "elasticsearch_index_optimization_jobs", validators = PositiveIntegerValidator.class)
    private int indexOptimizationJobs = 10;

    @Nullable
    public Period getMaxIndexRetentionPeriod() {
        return maxIndexRetentionPeriod;
    }

    public String getDefaultIndexPrefix() {
        return defaultIndexPrefix.toLowerCase(Locale.ENGLISH);
    }

    public String getDefaultIndexTemplateName() {
        return defaultIndexTemplateName;
    }

    public String getDefaultEventsIndexPrefix() {
        return defaultEventsIndexPrefix;
    }

    public String getDefaultSystemEventsIndexPrefix() {
        return defaultSystemEventsIndexPrefix;
    }


    public String getAnalyzer() {
        return analyzer;
    }

    public int getShards() {
        return shards;
    }

    public int getReplicas() {
        return replicas;
    }

    public int getIndexOptimizationMaxNumSegments() {
        return indexOptimizationMaxNumSegments;
    }

    public boolean isDisableIndexOptimization() {
        return disableIndexOptimization;
    }


    public Duration getIndexFieldTypePeriodicalFullRefreshInterval() {
        return indexFieldTypePeriodicalFullRefreshInterval;
    }

    public String getRotationStrategy() {
        return rotationStrategy;
    }

    public String getRetentionStrategy() {
        return retentionStrategy;
    }

    public Set<String> getDisabledRetentionStrategies() {
        return disabledRetentionStrategies;
    }

    public Period getMaxTimePerIndex() {
        return maxTimePerIndex;
    }

    public boolean isRotateEmptyIndex() {
        return rotateEmptyIndex;
    }

    public int getMaxDocsPerIndex() {
        return maxDocsPerIndex;
    }

    public long getMaxSizePerIndex() {
        return maxSizePerIndex;
    }

    public int getMaxNumberOfIndices() {
        return maxNumberOfIndices;
    }

    public Period getTimeSizeOptimizingRotationPeriod() {
        return timeSizeOptimizingRotationPeriod;
    }

    public Size getTimeSizeOptimizingRotationMinShardSize() {
        return timeSizeOptimizingRotationMinShardSize;
    }

    public Size getTimeSizeOptimizingRotationMaxShardSize() {
        return timeSizeOptimizingRotationMaxShardSize;
    }

    public Period getTimeSizeOptimizingRetentionMinLifeTime() {
        return timeSizeOptimizingRetentionMinLifeTime;
    }

    public Period getTimeSizeOptimizingRetentionMaxLifeTime() {
        return timeSizeOptimizingRetentionMaxLifeTime;
    }

    @Nullable
    public Period getTimeSizeOptimizingRetentionFixedLeeway() {
        return timeSizeOptimizingRetentionFixedLeeway;
    }

    public boolean isDisableVersionCheck() {
        return disableVersionCheck;
    }

    public Period getMaxWriteIndexAge() {
        return maxWriteIndexAge;
    }

    public List<String> getEnabledRotationStrategies() {
        return enabledRotationStrategies;
    }

    public boolean performRetention() {
        return !noRetention;
    }

    public Duration getIndexOptimizationTimeout() {
        return indexOptimizationTimeout;
    }

    public int getIndexOptimizationJobs() {
        return indexOptimizationJobs;
    }

    public boolean allowFlexibleRetentionPeriod() {
        return allowFlexibleRetentionPeriod;
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateTimeSizeOptimizingRotation() throws ValidationException {
        if (getTimeSizeOptimizingRotationMaxShardSize().compareTo(getTimeSizeOptimizingRotationMinShardSize()) < 0) {
            throw new ValidationException(f("\"%s=%s\" cannot be larger than \"%s=%s\"",
                    TIME_SIZE_OPTIMIZING_ROTATION_MIN_SHARD_SIZE, getTimeSizeOptimizingRotationMinShardSize(),
                    TIME_SIZE_OPTIMIZING_ROTATION_MAX_SHARD_SIZE, getTimeSizeOptimizingRotationMaxShardSize())
            );
        }
        Seconds timeSizeOptimizingRotationMaxLifeTimeSeconds = getTimeSizeOptimizingRetentionMaxLifeTime().toStandardSeconds();
        Seconds timeSizeOptimizingRotationMinLifeTimeSeconds = getTimeSizeOptimizingRetentionMinLifeTime().toStandardSeconds();
        if (timeSizeOptimizingRotationMaxLifeTimeSeconds.compareTo(timeSizeOptimizingRotationMinLifeTimeSeconds) <= 0) {
            throw new ValidationException(f("\"%s=%s\" needs to be larger than \"%s=%s\"",
                    TIME_SIZE_OPTIMIZING_RETENTION_MAX_LIFETIME, getTimeSizeOptimizingRetentionMaxLifeTime(),
                    TIME_SIZE_OPTIMIZING_RETENTION_MIN_LIFETIME, getTimeSizeOptimizingRetentionMinLifeTime())
            );
        }

        Seconds calculatedLeeway = timeSizeOptimizingRotationMaxLifeTimeSeconds.minus(timeSizeOptimizingRotationMinLifeTimeSeconds);
        if (getTimeSizeOptimizingRetentionFixedLeeway() != null &&
                calculatedLeeway.isLessThan(getTimeSizeOptimizingRetentionFixedLeeway().toStandardSeconds())) {
            throw new ValidationException(f("\"%s=%s\" and \"%s=%s\" leeway cannot be less than \"%s=%s\"",
                    TIME_SIZE_OPTIMIZING_RETENTION_MIN_LIFETIME, getTimeSizeOptimizingRetentionMinLifeTime(),
                    TIME_SIZE_OPTIMIZING_RETENTION_MAX_LIFETIME, getTimeSizeOptimizingRetentionMaxLifeTime(),
                    TIME_SIZE_OPTIMIZING_RETENTION_FIXED_LEEWAY, getTimeSizeOptimizingRetentionFixedLeeway())
            );
        }

        // If fixed leeway is defined, then calculated leeway is forced to that value by previous validation.
        // We don't need to repeat the fixed leeway check here.
        if (getMaxIndexRetentionPeriod() != null &&
                getMaxIndexRetentionPeriod().toStandardSeconds().isLessThan(
                        timeSizeOptimizingRotationMinLifeTimeSeconds.plus(calculatedLeeway))) {
            throw new ValidationException(f("\"%s=%s\" plus leeway=%s cannot to be larger than \"%s=%s\"",
                    TIME_SIZE_OPTIMIZING_RETENTION_MIN_LIFETIME, getTimeSizeOptimizingRetentionMinLifeTime(),
                    new Period(calculatedLeeway),
                    MAX_INDEX_RETENTION_PERIOD + " + leeway", getMaxIndexRetentionPeriod().plus(calculatedLeeway))
            );
        }
    }
}
