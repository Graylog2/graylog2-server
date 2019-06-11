package org.graylog.plugins.enterprise.search.searchtypes.pivot.buckets;

import com.fasterxml.jackson.annotation.JsonCreator;
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
    private static final double CK_DEFAULT_SCALINGFACTOR = 1.0;
    private static final String FIELD_SCALING = "scaling";
    public static final String type = "auto";

    @VisibleForTesting
    static final ImmutableRangeMap<Duration, DateHistogramInterval> boundaries = ImmutableRangeMap.<Duration, DateHistogramInterval>builder()
            .put(Range.atMost( Duration.ofMillis(20)), new DateHistogramInterval("1ms"))
            .put(Range.openClosed(Duration.ofMillis(20), Duration.ofMillis(200)), new DateHistogramInterval("5ms"))
            .put(Range.openClosed(Duration.ofMillis(200), Duration.ofMillis(500)), new DateHistogramInterval("10ms"))
            .put(Range.openClosed(Duration.ofMillis(500), Duration.ofMillis(1000)), new DateHistogramInterval("20ms"))
            .put(Range.openClosed(Duration.ofMillis(1000), Duration.ofSeconds(2)), new DateHistogramInterval("40ms"))
            .put(Range.openClosed(Duration.ofSeconds(2), Duration.ofSeconds(10)), new DateHistogramInterval("200ms"))
            .put(Range.openClosed(Duration.ofSeconds(10), Duration.ofSeconds(30)), new DateHistogramInterval("500ms"))
            .put(Range.openClosed(Duration.ofSeconds(30), Duration.ofMinutes(1)), DateHistogramInterval.seconds(1))
            .put(Range.openClosed(Duration.ofMinutes(1), Duration.ofMinutes(2)), DateHistogramInterval.seconds(2))
            .put(Range.openClosed(Duration.ofMinutes(2), Duration.ofMinutes(4)), DateHistogramInterval.seconds(5))
            .put(Range.openClosed(Duration.ofMinutes(4), Duration.ofMinutes(7)), DateHistogramInterval.seconds(10))
            .put(Range.openClosed(Duration.ofMinutes(7), Duration.ofMinutes(10)), DateHistogramInterval.seconds(20))
            .put(Range.openClosed(Duration.ofMinutes(10), Duration.ofMinutes(30)), DateHistogramInterval.seconds(30))
            .put(Range.openClosed(Duration.ofMinutes(30), Duration.ofHours(1)), DateHistogramInterval.MINUTE)
            .put(Range.openClosed(Duration.ofHours(1), Duration.ofHours(2)), DateHistogramInterval.minutes(2))
            .put(Range.openClosed(Duration.ofHours(2), Duration.ofHours(4)), DateHistogramInterval.minutes(5))
            .put(Range.openClosed(Duration.ofHours(4), Duration.ofHours(12)), DateHistogramInterval.minutes(10))
            .put(Range.openClosed(Duration.ofHours(12), Duration.ofHours(16)), DateHistogramInterval.minutes(15))
            .put(Range.openClosed(Duration.ofHours(16), Duration.ofDays(1)), DateHistogramInterval.minutes(30))
            .put(Range.openClosed(Duration.ofDays(1), Duration.ofDays(2)), DateHistogramInterval.HOUR)
            .put(Range.openClosed(Duration.ofDays(2), Duration.ofDays(4)), DateHistogramInterval.hours(2))
            .put(Range.openClosed(Duration.ofDays(4), Duration.ofDays(10)), DateHistogramInterval.hours(4))
            .put(Range.openClosed(Duration.ofDays(10), Duration.ofDays(12)), DateHistogramInterval.hours(6))
            .put(Range.openClosed(Duration.ofDays(12), Duration.ofDays(28)), DateHistogramInterval.hours(12))
            .put(Range.openClosed(Duration.ofDays(28), Duration.ofDays(60)), DateHistogramInterval.DAY)
            .put(Range.openClosed(Duration.ofDays(60), Duration.ofDays(120)), DateHistogramInterval.days(2))
            .put(Range.openClosed(Duration.ofDays(120), Duration.ofDays(180)), DateHistogramInterval.days(3))
            .put(Range.openClosed(Duration.ofDays(180), Duration.ofDays(365)), DateHistogramInterval.WEEK)
            .put(Range.open(Duration.ofDays(365), Duration.ofDays(730)), DateHistogramInterval.days(14))
            .put(Range.atLeast(Duration.ofDays(730)), DateHistogramInterval.MONTH)
            .build();

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty
    public abstract Double scaling();

    @Override
    public DateHistogramInterval toDateHistogramInterval(TimeRange timerange) {
        //noinspection UnnecessaryBoxing
        final Duration duration = Duration.ofMillis(
                Math.round(new Double(timerange.getTo().getMillis() - timerange.getFrom().getMillis()) / 1000 * scaling() * 1000)
        );
        return boundaries.get(duration);
    }

    public static AutoInterval create() {
        return create(type, CK_DEFAULT_SCALINGFACTOR);
    }

    public static AutoInterval create(double scalingFactor) {
        return new AutoValue_AutoInterval(type, scalingFactor);
    }

    @JsonCreator
    public static AutoInterval create(@JsonProperty(TYPE_FIELD) String type, @JsonProperty(FIELD_SCALING) @Nullable Double scalingFactor) {
        return create(Optional.ofNullable(scalingFactor).orElse(CK_DEFAULT_SCALINGFACTOR));
    }
}
