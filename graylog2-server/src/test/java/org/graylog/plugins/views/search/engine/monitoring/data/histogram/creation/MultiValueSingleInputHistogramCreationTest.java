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
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.Histogram;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.MultiValueBin;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.NamedBinDefinition;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.rest.resources.system.monitoring.MonitoringResource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.graylog.plugins.views.search.engine.monitoring.data.histogram.creation.MultiValueSingleInputHistogramCreation.OUTSIDE_AVAILABLE_BINS_BIN_NAME;
import static org.graylog2.rest.resources.system.monitoring.MonitoringResource.AVG_FUNCTION_NAME;
import static org.graylog2.rest.resources.system.monitoring.MonitoringResource.MAX_FUNCTION_NAME;
import static org.graylog2.rest.resources.system.monitoring.MonitoringResource.PERCENT_FUNCTION_NAME;
import static org.graylog2.rest.resources.system.monitoring.MonitoringResource.TIMERANGE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MultiValueSingleInputHistogramCreationTest {

    private MultiValueSingleInputHistogramCreation<Period, QueryExecutionStats> toTest;

    @BeforeEach
    void setUp() {
        Map<String, ValueComputation<QueryExecutionStats, Long>> valueFunctions = new LinkedHashMap<>();
        valueFunctions.put(MonitoringResource.AVG_FUNCTION_NAME, new AverageValueComputation<>(QueryExecutionStats::duration));
        valueFunctions.put(MonitoringResource.MAX_FUNCTION_NAME, new MaxValueComputation<>(QueryExecutionStats::duration));
        valueFunctions.put(MonitoringResource.PERCENT_FUNCTION_NAME, new PercentageValueComputation<>());
        toTest = new MultiValueSingleInputHistogramCreation<>(
                List.of(Period.hours(2), Period.hours(4), Period.days(2), Period.days(4)),
                new PeriodBasedBinChooser(),
                valueFunctions,
                TIMERANGE
        );
    }

    @Test
    void testCreatesProperHistogram() {
        final Collection<QueryExecutionStats> executionStats = List.of(
                getQueryExecutionStats(1, 1),
                getQueryExecutionStats(3, 1),
                getQueryExecutionStats(10, 3),
                getQueryExecutionStats(9, 3),
                getQueryExecutionStats(11, 3),
                getQueryExecutionStats(10, 3),
                getQueryExecutionStats(100, 70),
                getQueryExecutionStats(200, 71),
                getQueryExecutionStats(600, 72),
                getQueryExecutionStats(1234, 1111)
        );

        final Histogram histogram = toTest.create(executionStats);
        assertEquals(new Histogram(
                        List.of(TIMERANGE, AVG_FUNCTION_NAME, MAX_FUNCTION_NAME, PERCENT_FUNCTION_NAME),
                        List.of(
                                new MultiValueBin<>(new NamedBinDefinition("PT2H"), List.of(2L, 3L, 20L)),
                                new MultiValueBin<>(new NamedBinDefinition("PT4H"), List.of(10L, 11L, 40L)),
                                new MultiValueBin<>(new NamedBinDefinition("P2D"), List.of(0L, 0L, 0L)),
                                new MultiValueBin<>(new NamedBinDefinition("P4D"), List.of(300L, 600L, 30L)),
                                new MultiValueBin<>(new NamedBinDefinition(OUTSIDE_AVAILABLE_BINS_BIN_NAME), List.of(1234L, 1234L, 10L))
                        )
                ),
                histogram);
    }

    private QueryExecutionStats getQueryExecutionStats(long duration, int rangeInHours) {
        return QueryExecutionStats.builder()
                .duration(duration)
                .effectiveTimeRange(AbsoluteRange.create(
                        DateTime.now(DateTimeZone.UTC).minusHours(rangeInHours),
                        DateTime.now(DateTimeZone.UTC)))
                .build();
    }
}
