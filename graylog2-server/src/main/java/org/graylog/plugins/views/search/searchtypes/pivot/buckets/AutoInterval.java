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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
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
    static final ImmutableRangeMap<Duration, DateInterval> boundaries = ImmutableRangeMap.<Duration, DateInterval>builder()
            .put(Range.atMost( Duration.ofMillis(20)), new DateInterval(1, "ms"))
            .put(Range.openClosed(Duration.ofMillis(20), Duration.ofMillis(200)), new DateInterval(5, "ms"))
            .put(Range.openClosed(Duration.ofMillis(200), Duration.ofMillis(500)), new DateInterval(10, "ms"))
            .put(Range.openClosed(Duration.ofMillis(500), Duration.ofMillis(1000)), new DateInterval(20, "ms"))
            .put(Range.openClosed(Duration.ofMillis(1000), Duration.ofSeconds(2)), new DateInterval(40, "ms"))
            .put(Range.openClosed(Duration.ofSeconds(2), Duration.ofSeconds(10)), new DateInterval(200, "ms"))
            .put(Range.openClosed(Duration.ofSeconds(10), Duration.ofSeconds(30)), new DateInterval(500, "ms"))
            .put(Range.openClosed(Duration.ofSeconds(30), Duration.ofMinutes(1)), DateInterval.seconds(1))
            .put(Range.openClosed(Duration.ofMinutes(1), Duration.ofMinutes(2)), DateInterval.seconds(2))
            .put(Range.openClosed(Duration.ofMinutes(2), Duration.ofMinutes(4)), DateInterval.seconds(5))
            .put(Range.openClosed(Duration.ofMinutes(4), Duration.ofMinutes(7)), DateInterval.seconds(10))
            .put(Range.openClosed(Duration.ofMinutes(7), Duration.ofMinutes(10)), DateInterval.seconds(20))
            .put(Range.openClosed(Duration.ofMinutes(10), Duration.ofMinutes(30)), DateInterval.seconds(30))
            .put(Range.openClosed(Duration.ofMinutes(30), Duration.ofHours(1)), DateInterval.minutes(1))
            .put(Range.openClosed(Duration.ofHours(1), Duration.ofHours(2)), DateInterval.minutes(2))
            .put(Range.openClosed(Duration.ofHours(2), Duration.ofHours(4)), DateInterval.minutes(5))
            .put(Range.openClosed(Duration.ofHours(4), Duration.ofHours(12)), DateInterval.minutes(10))
            .put(Range.openClosed(Duration.ofHours(12), Duration.ofHours(16)), DateInterval.minutes(15))
            .put(Range.openClosed(Duration.ofHours(16), Duration.ofDays(1)), DateInterval.minutes(30))
            .put(Range.openClosed(Duration.ofDays(1), Duration.ofDays(2)), DateInterval.hours(1))
            .put(Range.openClosed(Duration.ofDays(2), Duration.ofDays(4)), DateInterval.hours(2))
            .put(Range.openClosed(Duration.ofDays(4), Duration.ofDays(10)), DateInterval.hours(4))
            .put(Range.openClosed(Duration.ofDays(10), Duration.ofDays(12)), DateInterval.hours(6))
            .put(Range.openClosed(Duration.ofDays(12), Duration.ofDays(28)), DateInterval.hours(12))
            .put(Range.openClosed(Duration.ofDays(28), Duration.ofDays(60)), DateInterval.days(1))
            .put(Range.openClosed(Duration.ofDays(60), Duration.ofDays(120)), DateInterval.days(2))
            .put(Range.openClosed(Duration.ofDays(120), Duration.ofDays(180)), DateInterval.days(3))
            .put(Range.openClosed(Duration.ofDays(180), Duration.ofDays(365)), DateInterval.weeks(1))
            .put(Range.open(Duration.ofDays(365), Duration.ofDays(730)), DateInterval.days(14))
            .put(Range.atLeast(Duration.ofDays(730)), DateInterval.months(1))
            .build();

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty
    public abstract Double scaling();

    @Override
    public DateInterval toDateInterval(TimeRange timerange) {
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
