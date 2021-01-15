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
package org.graylog.plugins.views.search.searchtypes.pivot.buckets;

import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class AutoIntervalTest {
    private final String result;
    private final String expectedResult;

    @Parameterized.Parameters(name = "{3}: Range of {0} should be {2} for scaling of {1}")
    public static Collection<Object[]> data() throws InvalidRangeParametersException {
        return Arrays.asList(new Object[][] {
                // Scaling Factor of 0.5
                {RelativeRange.create(1), 0.5, "10ms", "secondInterval"},
                {RelativeRange.create(300), 0.5, "5s", "defaultIntervalForSearch"},
                {RelativeRange.create(17 * 60), 0.5, "20s", "17 minutes"},
                {RelativeRange.create(8 * 24 * 60 * 60 + 2 * 60 * 60 + 13 * 60 + 2), 0.5, "4h", "8d2h13m2s"},
                {AbsoluteRange.create(DateTime.parse("2018-10-01T09:31:23.234Z"), DateTime.parse("2018-10-01T09:31:23.244Z")), 0.5, "1ms", "tenMillisecondIsTheLowerBoundary"},
                {AbsoluteRange.create(DateTime.parse("2017-10-01T09:31:23.235Z"), DateTime.parse("2018-10-01T09:31:23.235Z")), 0.5, "1w", "resultForOneYear"},
                {AbsoluteRange.create(DateTime.parse("2014-09-01T07:31:23.234Z"), DateTime.parse("2018-10-01T09:31:23.234Z")), 0.5, "1M", "fourYearsAreTheUpperBoundary"},
                {AbsoluteRange.create("2000-02-17T22:19:11.913Z", "2018-10-16T13:37:40.000Z"), 0.5, "1M", "veryLongDurationDoesNotReturnNull"},

                // Scaling Factor of 1/4
                {RelativeRange.create(1), 0.25, "10ms", "secondInterval"},
                {RelativeRange.create(300), 0.25, "2s", "defaultIntervalForSearch"},
                {RelativeRange.create(17 * 60), 0.25, "10s", "17 minutes"},
                {RelativeRange.create(8 * 24 * 60 * 60 + 2 * 60 * 60 + 13 * 60 + 2), 0.25, "2h", "8d2h13m2s"},
                {AbsoluteRange.create(DateTime.parse("2018-10-01T09:31:23.234Z"), DateTime.parse("2018-10-01T09:31:23.244Z")), 0.25, "1ms", "tenMillisecondIsTheLowerBoundary"},
                {AbsoluteRange.create(DateTime.parse("2017-10-01T09:31:23.235Z"), DateTime.parse("2018-10-01T09:31:23.235Z")), 0.25, "2d", "resultForOneYear"},
                {AbsoluteRange.create(DateTime.parse("2010-09-01T07:31:23.234Z"), DateTime.parse("2018-10-01T09:31:23.234Z")), 0.25, "1M", "eightYearsAreTheUpperBoundary"},
                {AbsoluteRange.create("2000-02-17T22:19:11.913Z", "2018-10-16T13:37:40.000Z"), 0.25, "1M", "veryLongDurationDoesNotReturnNull"},

                // Scaling Factor of 1
                {RelativeRange.create(1), 1, "20ms", "secondInterval"},
                {RelativeRange.create(300), 1, "10s", "defaultIntervalForSearch"},
                {RelativeRange.create(17 * 60), 1, "30s", "17 minutes"},
                {RelativeRange.create(8 * 24 * 60 * 60 + 2 * 60 * 60 + 13 * 60 + 2), 1, "4h", "8d2h13m2s"},
                {AbsoluteRange.create(DateTime.parse("2018-10-01T09:31:23.234Z"), DateTime.parse("2018-10-01T09:31:23.239Z")), 1, "1ms", "tenMillisecondIsTheLowerBoundary"},
                {AbsoluteRange.create(DateTime.parse("2017-10-01T09:31:23.235Z"), DateTime.parse("2018-10-01T09:31:23.235Z")), 1, "1w", "resultForOneYear"},
                {AbsoluteRange.create(DateTime.parse("2016-09-01T07:31:23.234Z"), DateTime.parse("2018-10-01T09:31:23.234Z")), 1, "1M", "twoYearsAreTheUpperBoundary"},
                {AbsoluteRange.create("2000-02-17T22:19:11.913Z", "2018-10-16T13:37:40.000Z"), 1, "1M", "veryLongDurationDoesNotReturnNull"},

                // Scaling Factor of 2
                {RelativeRange.create(1), 2, "40ms", "secondInterval"},
                {RelativeRange.create(300), 2, "20s", "defaultIntervalForSearch"},
                {RelativeRange.create(17 * 60), 2, "1m", "17 minutes"},
                {RelativeRange.create(8 * 24 * 60 * 60 + 2 * 60 * 60 + 13 * 60 + 2), 2, "12h", "8d2h13m2s"},
                {AbsoluteRange.create(DateTime.parse("2018-10-01T09:31:23.234Z"), DateTime.parse("2018-10-01T09:31:23.235Z")), 2, "1ms", "tenMillisecondIsTheLowerBoundary"},
                {AbsoluteRange.create(DateTime.parse("2017-10-01T09:31:23.235Z"), DateTime.parse("2018-10-01T09:31:23.235Z")), 2, "1M", "resultForOneYear"},
                {AbsoluteRange.create(DateTime.parse("2017-09-01T07:31:23.234Z"), DateTime.parse("2018-10-01T09:31:23.234Z")), 2, "1M", "oneYearIsTheUpperBoundary"},
                {AbsoluteRange.create("2000-02-17T22:19:11.913Z", "2018-10-16T13:37:40.000Z"), 2, "1M", "veryLongDurationDoesNotReturnNull"}
        });
    }

    public AutoIntervalTest(TimeRange range, double scalingFactor, String expectedResult, String description) {
        final AutoInterval autoInterval = AutoInterval.create(scalingFactor);
        this.result = autoInterval.toDateInterval(range).toString();
        this.expectedResult = expectedResult;
    }

    @Test
    public void test() throws Exception {
        assertThat(result).isEqualTo(expectedResult);
    }
}
