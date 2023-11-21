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
import org.joda.time.Period;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.rotation.tso.TimeSizeOptimizingValidator.periodOtherThanDays;
import static org.graylog2.indexer.rotation.tso.TimeSizeOptimizingValidator.validate;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TimeSizeOptimizingValidatorTest {

    @Mock
    private ElasticsearchConfiguration config;

    @Test
    public void validateLifetimeMaxIsShorterThanLifetimeMin() {
        assertThat(validate(config, Period.days(5), Period.days(4))).hasValueSatisfying(v ->
                assertThat(v.message()).contains("is shorter than index_lifetime_min")
        );
    }

    @Test
    public void validateMaxRetentionPeriod() {
        when(config.getTimeSizeOptimizingRotationPeriod()).thenReturn(Period.days(1));
        when(config.getMaxIndexRetentionPeriod()).thenReturn(Period.days(9));

        assertThat(validate(config, Period.days(2).withHours(2), Period.days(30))).hasValueSatisfying(v ->
                assertThat(v.message()).contains(
                        "Lifetime setting index_lifetime_max <P30D> exceeds the configured maximum of max_index_retention_period=P9D")
        );
    }

    @Test
    public void timeBasedSizeOptimizingOnlyWithMultipleOfDays() {
        when(config.getTimeSizeOptimizingRotationPeriod()).thenReturn(Period.days(1));

        assertThat(validate(config, Period.days(2).withHours(2), Period.days(30))).hasValueSatisfying(v ->
                assertThat(v.message()).contains(
                        "Lifetime setting index_lifetime_min <P2DT2H> can only be a multiple of days")
        );
    }

    @Test
    public void timeBasedSizeOptimizingHonorsFixedLeeWay() {
        when(config.getTimeSizeOptimizingRotationPeriod()).thenReturn(Period.days(1));
        when(config.getTimeSizeOptimizingRetentionFixedLeeway()).thenReturn(Period.days(10));


        assertThat(validate(config, Period.days(10), Period.days(19))).hasValueSatisfying(v -> assertThat(v.message())
                .contains("The duration between index_lifetime_max and index_lifetime_min <P9D> " +
                        "cannot be shorter than time_size_optimizing_retention_fixed_leeway <P10D>"));

        assertThat(validate(config, Period.days(10), Period.days(20))).isEmpty();
    }

    @Test
    public void testPeriodOtherThanDays() {
        assertThat(periodOtherThanDays(Period.days(5))).isFalse();
        assertThat(periodOtherThanDays(Period.weeks(5))).isTrue();
        assertThat(periodOtherThanDays(Period.days(5).withHours(3))).isTrue();
    }
}
