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

import org.graylog2.indexer.searches.timerangepresets.TimerangePreset;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class TimerangeOptionsToTimerangePresetsConversionTest {

    private TimerangeOptionsToTimerangePresetsConversion toTest;

    private PeriodToRelativeRangeConverter periodConverter;

    @BeforeEach
    void setUp() {
        periodConverter = mock(PeriodToRelativeRangeConverter.class);
        toTest = new TimerangeOptionsToTimerangePresetsConversion(periodConverter);
    }

    @Test
    void testConversionReturnsEmptyListOnEmptyInput() {
        assertThat(toTest.convert(Map.of())).isEmpty();
    }

    @Test
    void testConversionReturnsEmptyListOnNullInput() {
        assertThat(toTest.convert(null)).isEmpty();
    }

    @Test
    void testConversionCombinesDescriptionAndProperPeriod() {
        RelativeRange rangeFromConversion = RelativeRange.allTime();
        final Period periodToConvert = Period.years(6000);
        doReturn(rangeFromConversion).when(periodConverter).apply(periodToConvert);
        final List<TimerangePreset> result = toTest.convert(Map.of(periodToConvert, "Long, long time"));

        assertThat(result)
                .isNotNull()
                .hasSize(1)
                .extracting(TimerangePreset::timeRange, TimerangePreset::description)
                .containsOnly(
                        tuple(rangeFromConversion, "Long, long time")
                );

    }

    @Test
    void testConversionOnSomeDefaultRelativeTimerangeOptions() {
        Map<Period, String> defaults = new LinkedHashMap(
                Map.of(
                        Period.minutes(15), "15 minutes",
                        Period.hours(8), "8 hours",
                        Period.days(1), "1 day"
                )
        );

        doCallRealMethod().when(periodConverter).apply(any(Period.class));
        final List<TimerangePreset> result = toTest.convert(defaults);

        assertThat(result)
                .isNotNull()
                .hasSize(3)
                .extracting(TimerangePreset::timeRange, TimerangePreset::description)
                .containsExactlyInAnyOrder(
                        tuple(RelativeRange.Builder.builder()
                                .from(15 * 60)
                                .build(), "15 minutes"),
                        tuple(RelativeRange.Builder.builder()
                                .from(8 * 60 * 60)
                                .build(), "8 hours"),
                        tuple(RelativeRange.Builder.builder()
                                .from(24 * 60 * 60)
                                .build(), "1 day")
                );

    }


}
