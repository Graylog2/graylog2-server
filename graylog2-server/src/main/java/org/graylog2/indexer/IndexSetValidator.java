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
package org.graylog2.indexer;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategyConfig;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.plugin.rest.ValidationResult;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.DurationFieldType;
import org.joda.time.Period;

import javax.annotation.Nullable;

import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static org.graylog2.configuration.ElasticsearchConfiguration.TIME_SIZE_OPTIMIZING_RETENTION_FIXED_LEEWAY;
import static org.graylog2.configuration.ElasticsearchConfiguration.TIME_SIZE_OPTIMIZING_ROTATION_PERIOD;
import static org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategyConfig.INDEX_LIFETIME_MAX;
import static org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategyConfig.INDEX_LIFETIME_MIN;
import static org.graylog2.shared.utilities.StringUtils.f;

public class IndexSetValidator {
    private static final Duration MINIMUM_FIELD_TYPE_REFRESH_INTERVAL = Duration.standardSeconds(1L);
    private final IndexSetRegistry indexSetRegistry;
    private final ElasticsearchConfiguration elasticsearchConfiguration;

    @Inject
    public IndexSetValidator(IndexSetRegistry indexSetRegistry, ElasticsearchConfiguration elasticsearchConfiguration) {
        this.indexSetRegistry = indexSetRegistry;
        this.elasticsearchConfiguration = elasticsearchConfiguration;
    }

    public Optional<Violation> validate(IndexSetConfig newConfig) {

        // Don't validate prefix conflicts in case of an update
        if (Strings.isNullOrEmpty(newConfig.id())) {
            final Violation prefixViolation = validatePrefix(newConfig);
            if (prefixViolation != null) {
                return Optional.of(prefixViolation);
            }
        }

        final Violation refreshIntervalViolation = validateRefreshInterval(newConfig.fieldTypeRefreshInterval());
        if (refreshIntervalViolation != null) {
            return Optional.of(refreshIntervalViolation);
        }

        final Violation rotationViolation = validateRotation(newConfig.rotationStrategy());
        if (rotationViolation != null) {
            return Optional.of(rotationViolation);
        }

        final Violation retentionConfigViolation = validateRetentionConfig(newConfig.retentionStrategy());
        if (retentionConfigViolation != null) {
            return Optional.of(retentionConfigViolation);
        }

        return Optional.ofNullable(validateRetentionPeriod(newConfig.rotationStrategy(),
                newConfig.retentionStrategy()));
    }

    @Nullable
    public Violation validateRefreshInterval(Duration readableDuration) {
        // Ensure fieldTypeRefreshInterval is not shorter than a second, as that may impact performance
        if (readableDuration.isShorterThan(MINIMUM_FIELD_TYPE_REFRESH_INTERVAL)) {
            return Violation.create("Index field_type_refresh_interval \"" + readableDuration.toString() + "\" is too short. It must be 1 second or longer.");
        }
        return null;
    }

    @Nullable
    private Violation validatePrefix(IndexSetConfig newConfig) {
        // Build an example index name with the new prefix and check if this would be managed by an existing index set
        final String indexName = newConfig.indexPrefix() + MongoIndexSet.SEPARATOR + "0";
        if (indexSetRegistry.isManagedIndex(indexName)) {
            return Violation.create("Index prefix \"" + newConfig.indexPrefix() + "\" would conflict with an existing index set!");
        }

        // Check if the new index set configuration has a more generic index prefix than an existing index set,
        // or vice versa.
        // Example: new=graylog_foo existing=graylog => graylog is more generic so this is an error
        // Example: new=gray        existing=graylog => gray    is more generic so this is an error
        // This avoids problems with wildcard matching like "graylog_*".
        for (final IndexSet indexSet : indexSetRegistry) {
            if (newConfig.indexPrefix().startsWith(indexSet.getIndexPrefix()) || indexSet.getIndexPrefix().startsWith(newConfig.indexPrefix())) {
                return Violation.create("Index prefix \"" + newConfig.indexPrefix() + "\" would conflict with existing index set prefix \"" + indexSet.getIndexPrefix() + "\"");
            }
        }
        return null;
    }

    @Nullable
    public Violation validateRotation(RotationStrategyConfig rotationStrategyConfig) {
        if ((rotationStrategyConfig instanceof TimeBasedSizeOptimizingStrategyConfig config)) {
            final Period leeway = config.indexLifetimeMax().minus(config.indexLifetimeMin());
            if (leeway.toStandardSeconds().getSeconds() < 0) {
                return Violation.create(f("%s <%s> is shorter than %s <%s>", INDEX_LIFETIME_MAX, config.indexLifetimeMax(),
                        INDEX_LIFETIME_MIN, config.indexLifetimeMin()));
            }

            if (leeway.toStandardSeconds().isLessThan(elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod().toStandardSeconds())) {
                return Violation.create(f("The duration between %s and %s <%s> cannot be shorter than %s <%s>", INDEX_LIFETIME_MAX, INDEX_LIFETIME_MIN,
                        leeway, TIME_SIZE_OPTIMIZING_ROTATION_PERIOD, elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod()));
            }

            Period fixedLeeway = elasticsearchConfiguration.getTimeSizeOptimizingRetentionFixedLeeway();
            if (Objects.nonNull(fixedLeeway) && leeway.toStandardSeconds().isLessThan(fixedLeeway.toStandardSeconds())) {
                return Violation.create(f("The duration between %s and %s <%s> cannot be shorter than %s <%s>", INDEX_LIFETIME_MAX, INDEX_LIFETIME_MIN,
                        leeway, TIME_SIZE_OPTIMIZING_RETENTION_FIXED_LEEWAY, fixedLeeway));
            }


            final Period maxRetentionPeriod = elasticsearchConfiguration.getMaxIndexRetentionPeriod();
            if (maxRetentionPeriod != null
                    && config.indexLifetimeMax().toStandardSeconds().isGreaterThan(maxRetentionPeriod.toStandardSeconds())) {
                return Violation.create(f("Lifetime setting %s <%s> exceeds the configured maximum of %s=%s.",
                        INDEX_LIFETIME_MAX, config.indexLifetimeMax(),
                        ElasticsearchConfiguration.MAX_INDEX_RETENTION_PERIOD, maxRetentionPeriod));
            }

            if (periodOtherThanDays(config.indexLifetimeMax())) {
                return Violation.create(f("Lifetime setting %s <%s> can only be a multiple of days",
                        INDEX_LIFETIME_MAX, config.indexLifetimeMax()));
            }
            if (periodOtherThanDays(config.indexLifetimeMin())) {
                return Violation.create(f("Lifetime setting %s <%s> can only be a multiple of days",
                        INDEX_LIFETIME_MIN, config.indexLifetimeMin()));
            }
        }
        return null;
    }

    @VisibleForTesting
    boolean periodOtherThanDays(Period period) {
        return Arrays.stream(period.getFieldTypes())
                .filter(type -> !type.equals(DurationFieldType.days()))
                .anyMatch(type -> period.get(type) != 0);
    }

    @Nullable
    public Violation validateRetentionPeriod(RotationStrategyConfig rotationStrategyConfig, RetentionStrategyConfig retentionStrategyConfig) {
        final Period maxRetentionPeriod = elasticsearchConfiguration.getMaxIndexRetentionPeriod();

        if (maxRetentionPeriod == null) {
            return null;
        }

        if (!(rotationStrategyConfig instanceof TimeBasedRotationStrategyConfig)) {
            return null;
        }

        final Period rotationPeriod =
                ((TimeBasedRotationStrategyConfig) rotationStrategyConfig).rotationPeriod().normalizedStandard();

        final Period effectiveRetentionPeriod =
                rotationPeriod.multipliedBy(retentionStrategyConfig.maxNumberOfIndices()).normalizedStandard();

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        if (now.plus(effectiveRetentionPeriod).isAfter(now.plus(maxRetentionPeriod))) {
            return Violation.create(
                    f("Index retention setting %s=%d would result in an effective index retention period of %s. This exceeds the configured maximum of %s=%s.",
                            RetentionStrategyConfig.MAX_NUMBER_OF_INDEXES_FIELD, retentionStrategyConfig.maxNumberOfIndices(), effectiveRetentionPeriod,
                            ElasticsearchConfiguration.MAX_INDEX_RETENTION_PERIOD, maxRetentionPeriod));
        }

        return null;
    }

    @Nullable
    public Violation validateRetentionConfig(RetentionStrategyConfig retentionStrategyConfig) {
        ValidationResult validationResult = retentionStrategyConfig.validate(elasticsearchConfiguration);

        if (validationResult.failed()) {
            Optional<String> error = validationResult.getErrors().keySet().stream().findFirst();
            return Violation.create(error.orElse("Unknown retention config validation error"));
        }

        return null;
    }

    @AutoValue
    public abstract static class Violation {
        public abstract String message();

        public static Violation create(String message) {
            return new AutoValue_IndexSetValidator_Violation(message);
        }
    }
}
