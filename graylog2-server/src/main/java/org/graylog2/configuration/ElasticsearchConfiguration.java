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
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.documentation.DocumentationSection;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.util.Size;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.PositiveLongValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import com.google.common.annotations.VisibleForTesting;
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
@DocumentationSection(heading = "Indexer Settings", description = """
        Graylog uses Index Sets to manage settings for groups of indices. The default options for index sets are configurable
        for each index set in Graylog under System > Configuration > Index Set Defaults.
        The following settings are used to initialize in-database defaults on the first Graylog server startup.
        Specify these values if you want the Graylog server and indices to start with specific settings.
        """)
public class ElasticsearchConfiguration {
    public static final String MAX_INDEX_RETENTION_PERIOD = "max_index_retention_period";
    public static final String DEFAULT_EVENTS_INDEX_PREFIX = "default_events_index_prefix";
    public static final String DEFAULT_SYSTEM_EVENTS_INDEX_PREFIX = "default_system_events_index_prefix";
    public static final String TIME_SIZE_OPTIMIZING_RETENTION_MIN_LIFETIME = "time_size_optimizing_retention_min_lifetime";
    public static final String TIME_SIZE_OPTIMIZING_RETENTION_MAX_LIFETIME = "time_size_optimizing_retention_max_lifetime";
    public static final String TIME_SIZE_OPTIMIZING_RETENTION_FIXED_LEEWAY = "time_size_optimizing_retention_fixed_leeway";
    public static final String TIME_SIZE_OPTIMIZING_ROTATION_MIN_SHARD_SIZE = "time_size_optimizing_rotation_min_shard_size";
    public static final String TIME_SIZE_OPTIMIZING_ROTATION_MAX_SHARD_SIZE = "time_size_optimizing_rotation_max_shard_size";
    public static final String TIME_SIZE_OPTIMIZING_ROTATION_OS_MEMORY_FACTOR = "time_size_optimizing_rotation_os_memory_factor";
    public static final String TIME_SIZE_OPTIMIZING_ROTATION_PERIOD = "time_size_optimizing_rotation_period";
    public static final String ALLOW_FLEXIBLE_RETENTION_PERIOD = "allow_flexible_retention_period";
    public static final String INDEX_FIELD_TYPE_REFRESH_INTERVAL = "index_field_type_refresh_interval";

    // duplicated from the DataNode config, necessary to have the same number for the local query validation via Lucene
    @Documentation("This setting limits the number of clauses a Lucene BooleanQuery can have.")
    @Parameter(value = "opensearch_indices_query_bool_max_clause_count")
    private Integer indicesQueryBoolMaxClauseCount = 32768;

    @Documentation("""
            # Graylog uses Index Sets to manage settings for groups of indices. The default options for index sets are configurable
            # for each index set in Graylog under System > Configuration > Index Set Defaults.
            # The following settings are used to initialize in-database defaults on the first Graylog server startup.
            # Specify these values if you want the Graylog server and indices to start with specific settings.

            # The prefix for the Default Graylog index set.
            #
            """)
    @Parameter(value = "elasticsearch_index_prefix", required = true)
    private String defaultIndexPrefix = "graylog";

    @Documentation("The name of the index template for the Default Graylog index set.")
    @Parameter(value = "elasticsearch_template_name")
    private String defaultIndexTemplateName = "graylog-internal";

    @Documentation("The prefix for the for graylog event indices.")
    @Parameter(value = DEFAULT_EVENTS_INDEX_PREFIX, validators = StringNotBlankValidator.class)
    private String defaultEventsIndexPrefix = "gl-events";

    @Documentation("The prefix for graylog system event indices.")
    @Parameter(value = DEFAULT_SYSTEM_EVENTS_INDEX_PREFIX, validators = StringNotBlankValidator.class)
    private String defaultSystemEventsIndexPrefix = "gl-system-events";

    @Documentation("""
            Analyzer (tokenizer) to use for message and full_message field. The "standard" filter usually is a good idea.
            All supported analyzers are: standard, simple, whitespace, stop, keyword, pattern, language, snowball, custom
            Elasticsearch documentation: https://www.elastic.co/guide/en/elasticsearch/reference/2.3/analysis.html
            Note that this setting only takes effect on newly created indices.
            """)
    @Parameter(value = "elasticsearch_analyzer", required = true)
    private String analyzer = "standard";

    @Documentation("How many Elasticsearch shards should be used per index?")
    @Parameter(value = "elasticsearch_shards", validators = PositiveIntegerValidator.class, required = true)
    private int shards = 1;

    @Documentation("How many Elasticsearch replicas should be used per index?")
    @Parameter(value = "elasticsearch_replicas", validators = PositiveIntegerValidator.class, required = true)
    private int replicas = 0;

    @Documentation("""
            Disable the optimization of Elasticsearch indices after index cycling. This may take some load from Elasticsearch
            on heavily used systems with large indices, but it will decrease search performance. The default is to optimize
            cycled indices.
            """)
    @Parameter(value = "disable_index_optimization")
    private boolean disableIndexOptimization = false;

    @Documentation("""
            Optimize the index down to <= index_optimization_max_num_segments. A higher number may take some load from Elasticsearch
            on heavily used systems with large indices, but it will decrease search performance. The default is 1.
            """)
    @Parameter(value = "index_optimization_max_num_segments", validators = PositiveIntegerValidator.class)
    private int indexOptimizationMaxNumSegments = 1;

    @Documentation("""
            Time interval to refresh index field types.
            Default: 5s
            """)
    @Parameter(value = INDEX_FIELD_TYPE_REFRESH_INTERVAL, validators = {PositiveDurationValidator.class})
    private Duration indexFieldTypeRefreshInterval = Duration.seconds(5);

    @Documentation("""
            Time interval to trigger a full refresh of the index field types for all indexes. This will query ES for all indexes
            and populate any missing field type information to the database.
            """)
    @Parameter(value = "index_field_type_periodical_full_refresh_interval", validators = {PositiveDurationValidator.class})
    private Duration indexFieldTypePeriodicalFullRefreshInterval = Duration.minutes(5);

    @Documentation("""
            Decide what happens with the oldest indices when the maximum number of indices is reached.
            The following strategies are available:
              - delete # Deletes the index completely (Default)
              - close # Closes the index and hides it from the system. Can be re-opened later.
            """)
    @Parameter(value = "retention_strategy", required = true)
    private String retentionStrategy = DeletionRetentionStrategy.NAME;

    @Documentation("""
            This configuration list limits the retention strategies available for user configuration via the UI
            The following strategies can be disabled:
              - delete # Deletes the index completely (Default)
              - close # Closes the index and hides it from the system. Can be re-opened later.
              - none #  No operation is performed. The index stays open. (Not recommended)
            WARNING: At least one strategy must be enabled. Be careful when extending this list on existing installations!
            """)
    @Parameter(value = "disabled_retention_strategies", required = true, converter = StringSetConverter.class, validators = {RetentionStrategyValidator.class})
    private Set<String> disabledRetentionStrategies = Collections.emptySet();

    @Documentation("The default index rotation strategy to use.")
    @Parameter(value = "rotation_strategy", required = true)
    private String rotationStrategy = TimeBasedSizeOptimizingStrategy.NAME;

    @Documentation("""
            (Approximate) maximum time before a new Elasticsearch index is being created, also see
            no_retention and elasticsearch_max_number_of_indices. Default is 1 day.
            Configure this if you used 'rotation_strategy = time' above.
            Please note that this rotation period does not look at the time specified in the received messages, but is
            using the real clock value to decide when to rotate the index!
            Specify the time using a duration and a suffix indicating which unit you want:
              1w  = 1 week
              1d  = 1 day
              12h = 12 hours
            Permitted suffixes are: d for day, h for hour, m for minute, s for second.
            """)
    @Parameter(value = "elasticsearch_max_time_per_index", required = true)
    private Period maxTimePerIndex = Period.days(1);

    @Documentation("Controls whether empty indices are rotated. Only applies to the \"time\" rotation_strategy.")
    @Parameter(value = "elasticsearch_rotate_empty_index_set", required = true)
    private boolean rotateEmptyIndex = false;

    @Documentation("""
            (Approximate) maximum number of documents in an Elasticsearch index before a new index
            is being created, also see no_retention and elasticsearch_max_number_of_indices.
            Configure this if you used 'rotation_strategy = count' above.
            """)
    @Parameter(value = "elasticsearch_max_docs_per_index", validators = PositiveIntegerValidator.class, required = true)
    private int maxDocsPerIndex = 20000000;

    @Documentation("""
            (Approximate) maximum size in bytes per Elasticsearch index on disk before a new index is being created, also see
            no_retention and elasticsearch_max_number_of_indices. Default is 30GB.
            Configure this if you used 'rotation_strategy = size' above.
            """)
    @Parameter(value = "elasticsearch_max_size_per_index", validators = PositiveLongValidator.class, required = true)
    private long maxSizePerIndex = 30L * 1024 * 1024 * 1024; // 30GB

    @Documentation("How many indices do you want to keep for the delete and close retention types?")
    @Parameter(value = "elasticsearch_max_number_of_indices", required = true, validators = PositiveIntegerValidator.class)
    private int maxNumberOfIndices = 20;

    // TimeBasedSizeOptimizingStrategy Rotation
    @Documentation("tbd")
    @Parameter(value = TIME_SIZE_OPTIMIZING_ROTATION_PERIOD)
    private Period timeSizeOptimizingRotationPeriod = Period.days(1);

    @Documentation("tbd")
    @Parameter(value = TIME_SIZE_OPTIMIZING_ROTATION_MIN_SHARD_SIZE)
    private Size timeSizeOptimizingRotationMinShardSize;

    @Documentation("tbd")
    @Parameter(value = TIME_SIZE_OPTIMIZING_ROTATION_MAX_SHARD_SIZE)
    private Size timeSizeOptimizingRotationMaxShardSize;

    @Documentation("tbd")
    @Parameter(value = TIME_SIZE_OPTIMIZING_ROTATION_OS_MEMORY_FACTOR)
    private double timeSizeOptimizingRotationOSMemoryFactor = 0.6;

    @Documentation("tbd")
    @Parameter(value = TIME_SIZE_OPTIMIZING_RETENTION_MIN_LIFETIME)
    private Period timeSizeOptimizingRetentionMinLifeTime = IndexLifetimeConfig.DEFAULT_LIFETIME_MIN;

    @Documentation("tbd")
    @Parameter(value = TIME_SIZE_OPTIMIZING_RETENTION_MAX_LIFETIME)
    private Period timeSizeOptimizingRetentionMaxLifeTime = IndexLifetimeConfig.DEFAULT_LIFETIME_MAX;

    @Documentation("tbd")
    @Parameter(value = TIME_SIZE_OPTIMIZING_RETENTION_FIXED_LEEWAY)
    private Period timeSizeOptimizingRetentionFixedLeeway;

    @Documentation("tbd")
    @Parameter(value = ALLOW_FLEXIBLE_RETENTION_PERIOD)
    private boolean allowFlexibleRetentionPeriod = false;

    @Documentation("""
            Disable checking the version of Elasticsearch for being compatible with this Graylog release.
            WARNING: Using Graylog with unsupported and untested versions of Elasticsearch may lead to data loss!
            """)
    @Parameter(value = "elasticsearch_disable_version_check")
    private boolean disableVersionCheck = false;

    @Documentation("Optional upper bound on elasticsearch_max_time_per_index")
    @Parameter(value = "elasticsearch_max_write_index_age")
    private Period maxWriteIndexAge = null;

    @Documentation("Disable message retention on this node, i. e. disable Elasticsearch index rotation.")
    @Parameter(value = "no_retention")
    private boolean noRetention = false;

    @Documentation("""
            You can configure the default strategy used to determine when to rotate the currently active write index.
            Multiple rotation strategies are supported, the default being "time-size-optimizing":
              - "time-size-optimizing" tries to rotate daily, while focussing on optimal sized shards.
                The global default values can be configured with
                "time_size_optimizing_retention_min_lifetime" and "time_size_optimizing_retention_max_lifetime".
              - "count" of messages per index, use elasticsearch_max_docs_per_index below to configure
              - "size" per index, use elasticsearch_max_size_per_index below to configure
              - "time" interval between index rotations, use elasticsearch_max_time_per_index to configure
            A strategy may be disabled by specifying the optional enabled_index_rotation_strategies list and excluding that strategy.
            """)
    @Parameter(value = "enabled_index_rotation_strategies", converter = StringListConverter.class, validators = RotationStrategyValidator.class)
    private List<String> enabledRotationStrategies = Arrays.asList(
            TimeBasedRotationStrategy.NAME, MessageCountRotationStrategy.NAME,
            SizeBasedRotationStrategy.NAME, TimeBasedSizeOptimizingStrategy.NAME);

    @Documentation("""
            Provides a hard upper limit for the retention period of any index set at configuration time.

            This setting is used to validate the value a user chooses for the maximum number of retained indexes, when configuring
            an index set. However, it is only in effect, when a time-based rotation strategy is chosen.

            If a rotation strategy other than time-based is selected and/or no value is provided for this setting, no upper limit
            for index retention will be enforced. This is also the default.

            Default: none
            """)
    @Parameter(value = MAX_INDEX_RETENTION_PERIOD)
    private Period maxIndexRetentionPeriod = null;

    @Documentation("""
            Global timeout for index optimization (force merge) requests.
            Default: 1h
            """)
    @Parameter(value = "elasticsearch_index_optimization_timeout", validators = DurationCastedToIntegerValidator.class)
    private Duration indexOptimizationTimeout = Duration.hours(1L);

    @Documentation("""
            Maximum number of concurrently running index optimization (force merge) jobs.
            If you are using lots of different index sets, you might want to increase that number.
            This value should be set lower than elasticsearch_max_total_connections_per_route, otherwise index optimization
            could deplete all the client connections to the search server and block new messages ingestion for prolonged
            periods of time.
            Default: 10
            """)
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


    public Duration getIndexFieldTypeRefreshInterval() {
        return indexFieldTypeRefreshInterval;
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

    @Nullable
    public Size getTimeSizeOptimizingRotationMinShardSize() {
        return timeSizeOptimizingRotationMinShardSize;
    }

    @Nullable
    public Size getTimeSizeOptimizingRotationMaxShardSize() {
        return timeSizeOptimizingRotationMaxShardSize;
    }

    public double getTimeSizeOptimizingRotationOSMemoryFactor() {
        return timeSizeOptimizingRotationOSMemoryFactor;
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
        if (getTimeSizeOptimizingRotationMaxShardSize() != null && getTimeSizeOptimizingRotationMinShardSize() != null &&
                getTimeSizeOptimizingRotationMaxShardSize().compareTo(getTimeSizeOptimizingRotationMinShardSize()) < 0) {
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

    @VisibleForTesting
    public void setTimeSizeOptimizingRotationMinShardSize(Size timeSizeOptimizingRotationMinShardSize) {
        this.timeSizeOptimizingRotationMinShardSize = timeSizeOptimizingRotationMinShardSize;
    }

    @VisibleForTesting
    public void setTimeSizeOptimizingRotationMaxShardSize(Size timeSizeOptimizingRotationMaxShardSize) {
        this.timeSizeOptimizingRotationMaxShardSize = timeSizeOptimizingRotationMaxShardSize;
    }
}
