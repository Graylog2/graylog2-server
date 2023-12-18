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
package org.graylog2.indexer.rotation.tso;

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexSetValidator;
import org.joda.time.DurationFieldType;
import org.joda.time.Period;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static org.graylog2.configuration.ElasticsearchConfiguration.TIME_SIZE_OPTIMIZING_RETENTION_FIXED_LEEWAY;
import static org.graylog2.configuration.ElasticsearchConfiguration.TIME_SIZE_OPTIMIZING_ROTATION_PERIOD;
import static org.graylog2.indexer.rotation.tso.IndexLifetimeConfig.FIELD_INDEX_LIFETIME_MAX;
import static org.graylog2.indexer.rotation.tso.IndexLifetimeConfig.FIELD_INDEX_LIFETIME_MIN;
import static org.graylog2.shared.utilities.StringUtils.f;

public class TimeSizeOptimizingValidator {

    public static Optional<IndexSetValidator.Violation> validate(ElasticsearchConfiguration elasticsearchConfiguration,
                                                                 IndexLifetimeConfig retentionConfig) {
        Period indexLifetimeMin = retentionConfig.indexLifetimeMin();
        Period indexLifetimeMax = retentionConfig.indexLifetimeMax();
        final Period leeway = indexLifetimeMax.minus(indexLifetimeMin);
        if (leeway.toStandardSeconds().getSeconds() < 0) {
            return Optional.of(IndexSetValidator.Violation.create(f("%s <%s> is shorter than %s <%s>", FIELD_INDEX_LIFETIME_MAX, indexLifetimeMax,
                    FIELD_INDEX_LIFETIME_MIN, indexLifetimeMin)));
        }

        if (leeway.toStandardSeconds().isLessThan(elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod().toStandardSeconds())) {
            return Optional.of(IndexSetValidator.Violation.create(f("The duration between %s and %s <%s> cannot be shorter than %s <%s>", FIELD_INDEX_LIFETIME_MAX, FIELD_INDEX_LIFETIME_MIN,
                    leeway, TIME_SIZE_OPTIMIZING_ROTATION_PERIOD, elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod())));
        }

        Period fixedLeeway = elasticsearchConfiguration.getTimeSizeOptimizingRetentionFixedLeeway();
        if (Objects.nonNull(fixedLeeway) && leeway.toStandardSeconds().isLessThan(fixedLeeway.toStandardSeconds())) {
            return Optional.of(IndexSetValidator.Violation.create(f("The duration between %s and %s <%s> cannot be shorter than %s <%s>", FIELD_INDEX_LIFETIME_MAX, FIELD_INDEX_LIFETIME_MIN,
                    leeway, TIME_SIZE_OPTIMIZING_RETENTION_FIXED_LEEWAY, fixedLeeway)));
        }


        final Period maxRetentionPeriod = elasticsearchConfiguration.getMaxIndexRetentionPeriod();
        if (maxRetentionPeriod != null
                && indexLifetimeMax.toStandardSeconds().isGreaterThan(maxRetentionPeriod.toStandardSeconds())) {
            return Optional.of(IndexSetValidator.Violation.create(f("Lifetime setting %s <%s> exceeds the configured maximum of %s=%s.",
                    FIELD_INDEX_LIFETIME_MAX, indexLifetimeMax,
                    ElasticsearchConfiguration.MAX_INDEX_RETENTION_PERIOD, maxRetentionPeriod)));
        }

        if (periodOtherThanDays(indexLifetimeMax) && !elasticsearchConfiguration.allowFlexibleRetentionPeriod()) {
            return Optional.of(IndexSetValidator.Violation.create(f("Lifetime setting %s <%s> can only be a multiple of days",
                    FIELD_INDEX_LIFETIME_MAX, indexLifetimeMax)));
        }
        if (periodOtherThanDays(indexLifetimeMin) && !elasticsearchConfiguration.allowFlexibleRetentionPeriod()) {
            return Optional.of(IndexSetValidator.Violation.create(f("Lifetime setting %s <%s> can only be a multiple of days",
                    FIELD_INDEX_LIFETIME_MIN, indexLifetimeMin)));
        }
        return Optional.empty();
    }


    public static boolean periodOtherThanDays(Period period) {
        return Arrays.stream(period.getFieldTypes())
                .filter(type -> !type.equals(DurationFieldType.days()))
                .anyMatch(type -> period.get(type) != 0);
    }
}
