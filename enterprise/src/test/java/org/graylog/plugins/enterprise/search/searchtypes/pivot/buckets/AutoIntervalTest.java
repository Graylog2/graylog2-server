package org.graylog.plugins.enterprise.search.searchtypes.pivot.buckets;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class AutoIntervalTest {
    private AutoInterval autoInterval;

    @Before
    public void setUp() throws Exception {
        this.autoInterval = AutoInterval.create();
    }

    @Test
    public void secondInterval() throws Exception {
        final DateHistogramInterval result = this.autoInterval.toDateHistogramInterval(RelativeRange.create(1));

        assertThat(result.toString()).isEqualTo("40ms");
    }

    @Test
    public void minuteInterval() throws Exception {
        final DateHistogramInterval result = this.autoInterval.toDateHistogramInterval(RelativeRange.create(17 * 60));

        assertThat(result.toString()).isEqualTo("1m");
    }

    @Test
    public void twelveHourInterval() throws Exception {
        final DateHistogramInterval result = this.autoInterval.toDateHistogramInterval(RelativeRange.create(8 * 24 * 60 * 60 + 2 * 60 * 60 + 13 * 60 + 2));

        assertThat(result.toString()).isEqualTo("12h");
    }

    @Test
    public void tenMillisecondIsTheLowerBoundary() throws Exception {
        final DateTime from = DateTime.parse("2018-10-01T09:31:23.234Z");
        final DateTime to = DateTime.parse("2018-10-01T09:31:23.244Z");
        final DateHistogramInterval result = this.autoInterval.toDateHistogramInterval(AbsoluteRange.create(from, to));

        assertThat(result.toString()).isEqualTo("1ms");
    }

    @Test
    public void oneYearResultsInFourteenDays() throws Exception {
        final DateTime from = DateTime.parse("2017-10-01T09:31:23.235Z");
        final DateTime to = DateTime.parse("2018-10-01T09:31:23.235Z");
        final DateHistogramInterval result = this.autoInterval.toDateHistogramInterval(AbsoluteRange.create(from, to));

        assertThat(result.toString()).isEqualTo("1M");
    }

    @Test
    public void oneYearIsTheUpperBoundary() throws Exception {
        final DateTime from = DateTime.parse("2017-09-01T07:31:23.234Z");
        final DateTime to = DateTime.parse("2018-10-01T09:31:23.234Z");
        final DateHistogramInterval result = this.autoInterval.toDateHistogramInterval(AbsoluteRange.create(from, to));

        assertThat(result.toString()).isEqualTo("1M");
    }

    @Test
    public void veryLongDurationDoesNotReturnNull() throws InvalidRangeParametersException {
        final AbsoluteRange range = AbsoluteRange.create("2018-02-17T22:19:11.913Z", "2018-10-16T13:37:40.000Z");
        final DateHistogramInterval result = this.autoInterval.toDateHistogramInterval(range);

        assertThat(result).isNotNull();
        assertThat(result.toString()).isEqualTo("14d");
    }

    @Test
    public void rangeBoundariesMustBeConnectedAndSpanEverything() {
        final ImmutableMap<Range<Duration>, DateHistogramInterval> ranges = AutoInterval.boundaries.asMapOfRanges();
        final int rangeCount = ranges.size();
        final Range<Duration> firstRange = ranges.keySet().asList().get(0);
        assertThat(firstRange.hasLowerBound()).as("first range's lower bound should be unbounded").isFalse();

        final Range<Duration> lastRange = ranges.keySet().asList().get(rangeCount - 1);
        assertThat(lastRange.hasUpperBound()).as("last range's upper bound should be unbounded").isFalse();

        final Optional<Range<Duration>> ignored = ranges
                .keySet()
                .stream()
                .reduce((r1, r2) -> {
                    assertThat(r1.isConnected(r2))
                            .as("ranges %s and %s must be connected", r1, r2)
                            .isTrue();
                    return r2;
                });
    }
}