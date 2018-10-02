package org.graylog.plugins.enterprise.search.searchtypes.pivot.buckets;

import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

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

        assertThat(result.toString()).isEqualTo("14d");
    }

    @Test
    public void oneYearIsTheUpperBoundary() throws Exception {
        final DateTime from = DateTime.parse("2017-09-01T07:31:23.234Z");
        final DateTime to = DateTime.parse("2018-10-01T09:31:23.234Z");
        final DateHistogramInterval result = this.autoInterval.toDateHistogramInterval(AbsoluteRange.create(from, to));

        assertThat(result.toString()).isEqualTo("14d");
    }
}