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
package org.graylog.plugins.views.search.engine.monitoring.data.histogram.creation;

import org.graylog.plugins.views.search.engine.QueryExecutionStats;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodBasedBinChooserTest {

    private PeriodBasedBinChooser toTest;

    @BeforeEach
    void setUp() {
        toTest = new PeriodBasedBinChooser();
    }

    @Test
    void testReturnsEmptyOptionalOnNoAvailablePeriods() {
        assertTrue(
                toTest.chooseBin(
                        List.of(),
                        getQueryExecutionStats(5, AbsoluteRange.create(
                                DateTime.now(DateTimeZone.UTC).minusHours(5),
                                DateTime.now(DateTimeZone.UTC))
                        )
                ).isEmpty());
    }

    @Test
    void testReturnsEmptyOptionalWhenRangeExceedsLongestPeriod() {
        assertTrue(
                toTest.chooseBin(
                        List.of(Period.days(1), Period.days(2)),
                        getQueryExecutionStats(13, AbsoluteRange.create(
                                DateTime.now(DateTimeZone.UTC).minusDays(2).minusHours(1),
                                DateTime.now(DateTimeZone.UTC))
                        )
                ).isEmpty());
    }

    @Test
    void testChoosesProperPeriod() {
        final Optional<Period> chosenPeriod = toTest.chooseBin(
                List.of(Period.days(1), Period.days(2), Period.days(3)),
                getQueryExecutionStats(42, AbsoluteRange.create(
                        DateTime.now(DateTimeZone.UTC).minusDays(1).minusHours(23),
                        DateTime.now(DateTimeZone.UTC))
                )
        );
        assertTrue(chosenPeriod.isPresent());
        assertEquals(chosenPeriod.get(), Period.days(2));
    }

    private QueryExecutionStats getQueryExecutionStats(long duration, AbsoluteRange effectiveRange) {
        return QueryExecutionStats.builder()
                .duration(duration)
                .effectiveTimeRange(effectiveRange)
                .build();
    }
}
