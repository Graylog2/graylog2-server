package org.graylog.plugins.enterprise.search.searchtypes.pivot.buckets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSortedMap;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@AutoValue
@JsonTypeName(AutoInterval.type)
public abstract class AutoInterval implements Interval {
    public static final String type = "auto";
    private static final ImmutableSortedMap<Duration, DateHistogramInterval> boundaries = ImmutableSortedMap.<Duration, DateHistogramInterval>naturalOrder()
            .put(Duration.ofMinutes(5), DateHistogramInterval.seconds(10))
            .put(Duration.ofMinutes(15), DateHistogramInterval.seconds(30))
            .put(Duration.ofMinutes(30), DateHistogramInterval.MINUTE)
            .put(Duration.ofHours(1), DateHistogramInterval.minutes(2))
            .put(Duration.ofHours(2), DateHistogramInterval.minutes(5))
            .put(Duration.ofHours(6), DateHistogramInterval.minutes(10))
            .put(Duration.ofHours(8), DateHistogramInterval.minutes(15))
            .put(Duration.ofHours(12), DateHistogramInterval.minutes(30))
            .put(Duration.ofDays(1), DateHistogramInterval.HOUR)
            .put(Duration.ofDays(2), DateHistogramInterval.hours(2))
            .put(Duration.ofDays(5), DateHistogramInterval.hours(4))
            .put(Duration.ofDays(7), DateHistogramInterval.hours(6))
            .put(Duration.ofDays(14), DateHistogramInterval.hours(12))
            .put(Duration.ofDays(30), DateHistogramInterval.DAY)
            .put(Duration.ofDays(60), DateHistogramInterval.days(2))
            .put(Duration.ofDays(90), DateHistogramInterval.days(3))
            .put(Duration.ofDays(180), DateHistogramInterval.WEEK)
            // Time units larger than days cannot be larger than 1
            .put(Duration.ofDays(365), DateHistogramInterval.days(14))
            .build();

    @JsonProperty
    public abstract String type();

    @Override
    public DateHistogramInterval toDateHistogramInterval(TimeRange timerange) {
        //noinspection UnnecessaryBoxing
        final Duration duration = Duration.ofMillis(
                Math.round(new Double(timerange.getTo().getMillis() - timerange.getFrom().getMillis()) / 1000) * 1000
        );
        final Optional<DateHistogramInterval> result = boundaries.entrySet()
                .stream()
                .filter(entry -> entry.getKey().compareTo(duration) >= 0)
                .map(Map.Entry::getValue)
                .findFirst();
        return result.orElse(DateHistogramInterval.MONTH);
    }

    @JsonCreator
    public static AutoInterval create(@JsonProperty("type") String type) {
        return new AutoValue_AutoInterval(type);
    }
}
