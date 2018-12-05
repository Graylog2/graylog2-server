package org.graylog.plugins.enterprise.search.searchtypes.pivot.buckets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Optional;

@AutoValue
@JsonTypeName(AutoInterval.type)
public abstract class AutoInterval implements Interval {
    private static final double CK_DEFAULT_SCALINGFACTOR = 0.5;
    public static final String type = "auto";
    @VisibleForTesting
    static final ImmutableRangeMap<Duration, DateHistogramInterval> boundaries = ImmutableRangeMap.<Duration, DateHistogramInterval>builder()
            .put(Range.atMost( Duration.ofMillis(10)), new DateHistogramInterval("1ms"))
            .put(Range.openClosed(Duration.ofMillis(10), Duration.ofMillis(100)), new DateHistogramInterval("5ms"))
            .put(Range.openClosed(Duration.ofMillis(100), Duration.ofMillis(250)), new DateHistogramInterval("10ms"))
            .put(Range.openClosed(Duration.ofMillis(250), Duration.ofMillis(500)), new DateHistogramInterval("20ms"))
            .put(Range.openClosed(Duration.ofMillis(500), Duration.ofSeconds(1)), new DateHistogramInterval("40ms"))
            .put(Range.openClosed(Duration.ofSeconds(1), Duration.ofSeconds(5)), new DateHistogramInterval("200ms"))
            .put(Range.openClosed(Duration.ofSeconds(5), Duration.ofSeconds(15)), new DateHistogramInterval("500ms"))
            .put(Range.openClosed(Duration.ofSeconds(15), Duration.ofSeconds(30)), DateHistogramInterval.seconds(1))
            .put(Range.openClosed(Duration.ofSeconds(30), Duration.ofMinutes(1)), DateHistogramInterval.seconds(2))
            .put(Range.openClosed(Duration.ofMinutes(1), Duration.ofMinutes(2)), DateHistogramInterval.seconds(5))
            .put(Range.openClosed(Duration.ofMinutes(2), Duration.ofMinutes(5)), DateHistogramInterval.seconds(10))
            .put(Range.openClosed(Duration.ofMinutes(5), Duration.ofMinutes(15)), DateHistogramInterval.seconds(30))
            .put(Range.openClosed(Duration.ofMinutes(15), Duration.ofMinutes(30)), DateHistogramInterval.MINUTE)
            .put(Range.openClosed(Duration.ofMinutes(30), Duration.ofHours(1)), DateHistogramInterval.minutes(2))
            .put(Range.openClosed(Duration.ofHours(1), Duration.ofHours(2)), DateHistogramInterval.minutes(5))
            .put(Range.openClosed(Duration.ofHours(2), Duration.ofHours(6)), DateHistogramInterval.minutes(10))
            .put(Range.openClosed(Duration.ofHours(6), Duration.ofHours(8)), DateHistogramInterval.minutes(15))
            .put(Range.openClosed(Duration.ofHours(8), Duration.ofHours(12)), DateHistogramInterval.minutes(30))
            .put(Range.openClosed(Duration.ofHours(12), Duration.ofDays(1)), DateHistogramInterval.HOUR)
            .put(Range.openClosed(Duration.ofDays(1), Duration.ofDays(2)), DateHistogramInterval.hours(2))
            .put(Range.openClosed(Duration.ofDays(2), Duration.ofDays(5)), DateHistogramInterval.hours(4))
            .put(Range.openClosed(Duration.ofDays(5), Duration.ofDays(7)), DateHistogramInterval.hours(6))
            .put(Range.openClosed(Duration.ofDays(7), Duration.ofDays(14)), DateHistogramInterval.hours(12))
            .put(Range.openClosed(Duration.ofDays(14), Duration.ofDays(30)), DateHistogramInterval.DAY)
            .put(Range.openClosed(Duration.ofDays(30), Duration.ofDays(60)), DateHistogramInterval.days(2))
            .put(Range.openClosed(Duration.ofDays(60), Duration.ofDays(90)), DateHistogramInterval.days(3))
            .put(Range.openClosed(Duration.ofDays(90), Duration.ofDays(180)), DateHistogramInterval.WEEK)
            .put(Range.open(Duration.ofDays(180), Duration.ofDays(365)), DateHistogramInterval.days(14))
            .put(Range.atLeast(Duration.ofDays(365)), DateHistogramInterval.MONTH)
            .build();

    @JsonProperty
    @Override
    public abstract String type();

    @JsonIgnore
    public abstract Double scaling();

    @Override
    public DateHistogramInterval toDateHistogramInterval(TimeRange timerange) {
        //noinspection UnnecessaryBoxing
        final Duration duration = Duration.ofMillis(
                Math.round(new Double(timerange.getTo().getMillis() - timerange.getFrom().getMillis()) / 1000 * scaling()) * 1000
        );
        return boundaries.get(duration);
    }

    @JsonCreator
    public static AutoInterval create() {
        return create(null);
    }

    public static AutoInterval create(@Nullable Double scalingFactor) { return new AutoValue_AutoInterval(type, Optional.ofNullable(scalingFactor).orElse(CK_DEFAULT_SCALINGFACTOR)); }
}
