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
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.PositiveLongValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import org.joda.time.Period;

import java.util.Locale;

public class ElasticsearchConfiguration {
    public static final String DEFAULT_EVENTS_INDEX_PREFIX = "default_events_index_prefix";
    public static final String DEFAULT_SYSTEM_EVENTS_INDEX_PREFIX = "default_system_events_index_prefix";

    @Parameter(value = "elasticsearch_disable_version_check")
    private boolean disableVersionCheck = false;

    @Deprecated // Should be removed in Graylog 3.0
    @Parameter(value = "elasticsearch_index_prefix", required = true)
    private String indexPrefix = "graylog";

    @Deprecated // Should be removed in Graylog 3.0
    @Parameter(value = "elasticsearch_max_number_of_indices", required = true, validator = PositiveIntegerValidator.class)
    private int maxNumberOfIndices = 20;

    @Deprecated // Should be removed in Graylog 3.0
    @Parameter(value = "elasticsearch_max_docs_per_index", validator = PositiveIntegerValidator.class, required = true)
    private int maxDocsPerIndex = 20000000;

    @Deprecated // Should be removed in Graylog 3.0
    @Parameter(value = "elasticsearch_max_size_per_index", validator = PositiveLongValidator.class, required = true)
    private long maxSizePerIndex = 1L * 1024 * 1024 * 1024; // 1GB

    @Deprecated // Should be removed in Graylog 3.0
    @Parameter(value = "elasticsearch_max_time_per_index", required = true)
    private Period maxTimePerIndex = Period.days(1);

    @Deprecated // Should be removed in Graylog 3.0
    @Parameter(value = "elasticsearch_shards", validator = PositiveIntegerValidator.class, required = true)
    private int shards = 4;

    @Deprecated // Should be removed in Graylog 3.0
    @Parameter(value = "elasticsearch_replicas", validator = PositiveIntegerValidator.class, required = true)
    private int replicas = 0;

    @Deprecated // Should be removed in Graylog 3.0
    @Parameter(value = "elasticsearch_analyzer", required = true)
    private String analyzer = "standard";

    @Deprecated // Should be removed in Graylog 3.0
    @Parameter(value = "elasticsearch_template_name")
    private String templateName = "graylog-internal";

    @Parameter(value = "no_retention")
    private boolean noRetention = false;

    @Deprecated // Should be removed in Graylog 3.0
    @Parameter(value = "retention_strategy", required = true)
    private String retentionStrategy = "delete";

    @Deprecated // Should be removed in Graylog 3.0
    @Parameter(value = "rotation_strategy")
    private String rotationStrategy = "count";

    @Deprecated // Should be removed in Graylog 3.0
    @Parameter(value = "disable_index_optimization")
    private boolean disableIndexOptimization = false;

    @Deprecated // Should be removed in Graylog 3.0
    @Parameter(value = "index_optimization_max_num_segments", validator = PositiveIntegerValidator.class)
    private int indexOptimizationMaxNumSegments = 1;

    @Parameter(value = "elasticsearch_index_optimization_timeout", validator = PositiveDurationValidator.class)
    private Duration indexOptimizationTimeout = Duration.hours(1L);

    @Parameter(value = "elasticsearch_index_optimization_jobs", validator = PositiveIntegerValidator.class)
    private int indexOptimizationJobs = 20;

    @Parameter(value = "index_field_type_periodical_interval", validator = PositiveDurationValidator.class)
    private Duration indexFieldTypePeriodicalInterval = Duration.hours(1L);

    @Parameter(value = DEFAULT_EVENTS_INDEX_PREFIX, validators = StringNotBlankValidator.class)
    private String defaultEventsIndexPrefix = "gl-events";

    @Parameter(value = DEFAULT_SYSTEM_EVENTS_INDEX_PREFIX, validators = StringNotBlankValidator.class)
    private String defaultSystemEventsIndexPrefix = "gl-system-events";

    public boolean isDisableVersionCheck() {
        return disableVersionCheck;
    }

    @Deprecated // Should be removed in Graylog 3.0
    public String getIndexPrefix() {
        return indexPrefix.toLowerCase(Locale.ENGLISH);
    }

    @Deprecated // Should be removed in Graylog 3.0
    public int getMaxNumberOfIndices() {
        return maxNumberOfIndices;
    }

    @Deprecated // Should be removed in Graylog 3.0
    public int getMaxDocsPerIndex() {
        return maxDocsPerIndex;
    }

    @Deprecated // Should be removed in Graylog 3.0
    public long getMaxSizePerIndex() {
        return maxSizePerIndex;
    }

    @Deprecated // Should be removed in Graylog 3.0
    public Period getMaxTimePerIndex() {
        return maxTimePerIndex;
    }

    @Deprecated // Should be removed in Graylog 3.0
    public int getShards() {
        return shards;
    }

    @Deprecated // Should be removed in Graylog 3.0
    public int getReplicas() {
        return replicas;
    }

    @Deprecated // Should be removed in Graylog 3.0
    public String getAnalyzer() {
        return analyzer;
    }

    @Deprecated // Should be removed in Graylog 3.0
    public String getTemplateName() {
        return templateName;
    }

    @Deprecated // Should be removed in Graylog 3.0
    public String getRotationStrategy() {
        return rotationStrategy;
    }

    public boolean performRetention() {
        return !noRetention;
    }

    @Deprecated // Should be removed in Graylog 3.0
    public String getRetentionStrategy() {
        return retentionStrategy;
    }

    @Deprecated // Should be removed in Graylog 3.0
    public int getIndexOptimizationMaxNumSegments() {
        return indexOptimizationMaxNumSegments;
    }

    @Deprecated // Should be removed in Graylog 3.0
    public boolean isDisableIndexOptimization() {
        return disableIndexOptimization;
    }

    public Duration getIndexOptimizationTimeout() {
        return indexOptimizationTimeout;
    }

    public int getIndexOptimizationJobs() {
        return indexOptimizationJobs;
    }

    public Duration getIndexFieldTypePeriodicalInterval() {
        return indexFieldTypePeriodicalInterval;
    }

    public String getDefaultEventsIndexPrefix() {
        return defaultEventsIndexPrefix;
    }

    public String getDefaultSystemEventsIndexPrefix() {
        return defaultSystemEventsIndexPrefix;
    }
}
