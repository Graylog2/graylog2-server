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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.converters.StringListConverter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.FilePathReadableValidator;
import com.github.joschi.jadconfig.validators.InetPortValidator;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.PositiveLongValidator;
import org.joda.time.Period;

import javax.validation.constraints.NotNull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ElasticsearchConfiguration {
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

    @Parameter(value = "elasticsearch_request_timeout", validator = PositiveDurationValidator.class)
    private Duration requestTimeout = Duration.minutes(1L);

    @Parameter(value = "elasticsearch_index_optimization_timeout", validator = PositiveDurationValidator.class)
    private Duration indexOptimizationTimeout = Duration.hours(1L);

    @Parameter(value = "elasticsearch_index_optimization_jobs", validator = PositiveIntegerValidator.class)
    private int indexOptimizationJobs = 20;

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

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public Duration getIndexOptimizationTimeout() {
        return indexOptimizationTimeout;
    }

    public int getIndexOptimizationJobs() {
        return indexOptimizationJobs;
    }
}
