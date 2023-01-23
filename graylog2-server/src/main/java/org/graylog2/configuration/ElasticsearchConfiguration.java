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
import com.github.joschi.jadconfig.converters.StringListConverter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.util.Size;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.PositiveLongValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import org.graylog2.configuration.validators.RotationStrategyValidator;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategy;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategy;
import org.joda.time.Period;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class ElasticsearchConfiguration {
    public static final String MAX_INDEX_RETENTION_PERIOD = "max_index_retention_period";
    public static final String DEFAULT_EVENTS_INDEX_PREFIX = "default_events_index_prefix";
    public static final String DEFAULT_SYSTEM_EVENTS_INDEX_PREFIX = "default_system_events_index_prefix";

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

    @Parameter(value = "elasticsearch_shards", validator = PositiveIntegerValidator.class, required = true)
    private int shards = 1;

    @Parameter(value = "elasticsearch_replicas", validator = PositiveIntegerValidator.class, required = true)
    private int replicas = 0;

    @Parameter(value = "disable_index_optimization")
    private boolean disableIndexOptimization = false;

    @Parameter(value = "index_optimization_max_num_segments", validator = PositiveIntegerValidator.class)
    private int indexOptimizationMaxNumSegments = 1;

    @Parameter(value = "index_field_type_periodical_full_refresh_interval", validators = {PositiveDurationValidator.class})
    private Duration indexFieldTypePeriodicalFullRefreshInterval = Duration.minutes(5);

    @Parameter(value = "retention_strategy", required = true)
    private String retentionStrategy = DeletionRetentionStrategy.NAME;

    @Parameter(value = "rotation_strategy", required = true)
    private String rotationStrategy = SizeBasedRotationStrategy.NAME;

    // Rotation
    @Parameter(value = "elasticsearch_max_time_per_index", required = true)
    private Period maxTimePerIndex = Period.days(1);

    // Rotation
    @Parameter(value = "elasticsearch_rotate_empty_index_set", required = true)
    private boolean rotateEmptyIndex = false;

    // Rotation
    @Parameter(value = "elasticsearch_max_docs_per_index", validator = PositiveIntegerValidator.class, required = true)
    private int maxDocsPerIndex = 20000000;

    // Rotation
    @Parameter(value = "elasticsearch_max_size_per_index", validator = PositiveLongValidator.class, required = true)
    private long maxSizePerIndex = 30L * 1024 * 1024 * 1024; // 30GB

    // Retention
    @Parameter(value = "elasticsearch_max_number_of_indices", required = true, validator = PositiveIntegerValidator.class)
    private int maxNumberOfIndices = 20;

    // TimeBasedSizeOptimizingStrategy Rotation
    @Parameter(value = "time_size_optimizing_rotation_period")
    private Period timeSizeOptimizingRotationPeriod = Period.days(1);

    @Parameter(value = "time_size_optimizing_rotation_min_size")
    private Size timeSizeOptimizingRotationMinSize = Size.gigabytes(20);

    @Parameter(value = "time_size_optimizing_rotation_max_size")
    private Size timeSizeOptimizingRotationMaxSize = Size.gigabytes(50);

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

    @Nullable
    public Period getMaxIndexRetentionPeriod() {
        return maxIndexRetentionPeriod;
    }

    @Parameter(value = "elasticsearch_index_optimization_timeout", validator = DurationCastedToIntegerValidator.class)
    private Duration indexOptimizationTimeout = Duration.hours(1L);

    @Parameter(value = "elasticsearch_index_optimization_jobs", validator = PositiveIntegerValidator.class)
    private int indexOptimizationJobs = 10;

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

    public Size getTimeSizeOptimizingRotationMinSize() {
        return timeSizeOptimizingRotationMinSize;
    }

    public Size getTimeSizeOptimizingRotationMaxSize() {
        return timeSizeOptimizingRotationMaxSize;
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
}
