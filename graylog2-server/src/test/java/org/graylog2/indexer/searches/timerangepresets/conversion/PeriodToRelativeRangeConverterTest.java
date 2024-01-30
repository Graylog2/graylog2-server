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
package org.graylog2.indexer.searches.timerangepresets.conversion;

import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

class PeriodToRelativeRangeConverterTest {

    private PeriodToRelativeRangeConverter converter;

    @BeforeEach
    void setUp() {
        converter = new PeriodToRelativeRangeConverter();
    }

    @Test
    void testReturnsNullOnNullInput() {
        assertNull(converter.apply(null));
    }

    @Test
    void testSecondConversion() {
        final RelativeRange result = converter.apply(Period.seconds(5));
        verifyResult(result, 5);
    }

    @Test
    void testMinuteConversion() {
        final RelativeRange result = converter.apply(Period.minutes(30));
        verifyResult(result, 1800);
    }

    @Test
    void testHourConversion() {
        final RelativeRange result = converter.apply(Period.hours(2));
        verifyResult(result, 7200);
    }

    @Test
    void testDayConversion() {
        final RelativeRange result = converter.apply(Period.days(2));
        verifyResult(result, 172800);
    }

    @Test
    void testMixedPeriodConversion() {
        final RelativeRange result = converter.apply(Period.hours(1).plusMinutes(10).plusSeconds(7));
        verifyResult(result, 4207);
    }

    @Test
    void testMonthsPeriodConversion() {
        final RelativeRange result = converter.apply(Period.months(2));
        verifyResult(result, 2 * 30 * 24 * 60 * 60);
    }

    @Test
    void testYearsPeriodConversion() {
        final RelativeRange result = converter.apply(Period.years(3));
        verifyResult(result, 3 * 365 * 24 * 60 * 60);
    }

    @Test
    void testYearsMonthsPeriodConversion() {
        final RelativeRange result = converter.apply(Period.years(5).plusMonths(1));
        verifyResult(result, (5 * 365 + 1 * 30) * 24 * 60 * 60);
    }

    @Test
    void testYearsMonthsMixedPeriodConversion() {
        final RelativeRange result = converter.apply(Period.years(5).plusMonths(1).plusHours(1).plusMinutes(10).plusSeconds(7));
        verifyResult(result, ((5 * 365 + 1 * 30) * 24 * 60 * 60) + (60 * 60) + (10 * 60) + 7);
    }

    private void verifyResult(final RelativeRange result, final int expectedFromField) {
        assertThat(result)
                .isNotNull()
                .satisfies(range -> {
                    assertThat(range.range()).isEmpty();
                    assertThat(range.from()).isPresent().hasValue(expectedFromField);
                });
    }

}
