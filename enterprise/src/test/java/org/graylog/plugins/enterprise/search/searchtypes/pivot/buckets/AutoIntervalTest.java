package org.graylog.plugins.enterprise.search.searchtypes.pivot.buckets;

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

    @Parameterized.Parameters(name = "{3}")
    public static Collection<Object[]> data() throws InvalidRangeParametersException {
        return Arrays.asList(new Object[][] {
                // Scaling Factor of 1
                {RelativeRange.create(1), 1, "40ms", "secondInterval"},
                {RelativeRange.create(17 * 60), 1, "1m", "minuteInterval"},
                {RelativeRange.create(8 * 24 * 60 * 60 + 2 * 60 * 60 + 13 * 60 + 2), 1, "12h", "twelveHourInterval"},
                {AbsoluteRange.create(DateTime.parse("2018-10-01T09:31:23.234Z"), DateTime.parse("2018-10-01T09:31:23.244Z")), 1, "1ms", "tenMillisecondIsTheLowerBoundary"},
                {AbsoluteRange.create(DateTime.parse("2017-10-01T09:31:23.235Z"), DateTime.parse("2018-10-01T09:31:23.235Z")), 1, "1M", "oneYearResultsInOneMonth"},
                {AbsoluteRange.create(DateTime.parse("2017-09-01T07:31:23.234Z"), DateTime.parse("2018-10-01T09:31:23.234Z")), 1, "1M", "oneYearIsTheUpperBoundary"},
                {AbsoluteRange.create("2018-02-17T22:19:11.913Z", "2018-10-16T13:37:40.000Z"), 1, "14d", "veryLongDurationDoesNotReturnNull"},

                // Scaling Factor of 0.5
                {RelativeRange.create(1), 0.5, "40ms", "secondInterval"},
                {RelativeRange.create(17 * 60), 0.5, "30s", "minuteInterval"},
                {RelativeRange.create(8 * 24 * 60 * 60 + 2 * 60 * 60 + 13 * 60 + 2), 0.5, "4h", "twelveHourInterval"},
                {AbsoluteRange.create(DateTime.parse("2018-10-01T09:31:23.234Z"), DateTime.parse("2018-10-01T09:31:23.244Z")), 0.5, "1ms", "tenMillisecondIsTheLowerBoundary"},
                {AbsoluteRange.create(DateTime.parse("2017-10-01T09:31:23.235Z"), DateTime.parse("2018-10-01T09:31:23.235Z")), 0.5, "14d", "oneYearResultsInOneMonth"},
                {AbsoluteRange.create(DateTime.parse("2017-09-01T07:31:23.234Z"), DateTime.parse("2018-10-01T09:31:23.234Z")), 0.5, "14d", "oneYearIsTheUpperBoundary"},
                {AbsoluteRange.create("2018-02-17T22:19:11.913Z", "2018-10-16T13:37:40.000Z"), 0.5, "1w", "veryLongDurationDoesNotReturnNull"},

                // Scaling Factor of 0.5
                {RelativeRange.create(1), 2, "200ms", "secondInterval"},
                {RelativeRange.create(17 * 60), 2, "2m", "minuteInterval"},
                {RelativeRange.create(8 * 24 * 60 * 60 + 2 * 60 * 60 + 13 * 60 + 2), 2, "1d", "twelveHourInterval"},
                {AbsoluteRange.create(DateTime.parse("2018-10-01T09:31:23.234Z"), DateTime.parse("2018-10-01T09:31:23.244Z")), 2, "1ms", "tenMillisecondIsTheLowerBoundary"},
                {AbsoluteRange.create(DateTime.parse("2017-10-01T09:31:23.235Z"), DateTime.parse("2018-10-01T09:31:23.235Z")), 2, "1M", "oneYearResultsInOneMonth"},
                {AbsoluteRange.create(DateTime.parse("2017-09-01T07:31:23.234Z"), DateTime.parse("2018-10-01T09:31:23.234Z")), 2, "1M", "oneYearIsTheUpperBoundary"},
                {AbsoluteRange.create("2018-02-17T22:19:11.913Z", "2018-10-16T13:37:40.000Z"), 2, "1M", "veryLongDurationDoesNotReturnNull"}
        });
    }

    public AutoIntervalTest(TimeRange range, double scalingFactor, String expectedResult, String description) {
        final AutoInterval autoInterval = AutoInterval.create(scalingFactor);
        this.result = autoInterval.toDateHistogramInterval(range).toString();
        this.expectedResult = expectedResult;
    }

    @Test
    public void test() throws Exception {
        assertThat(result).isEqualTo(expectedResult);
    }
}