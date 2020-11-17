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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.AbsoluteRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.RelativeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AutoInterval;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Interval;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.TimeUnitInterval;

import java.time.Duration;

public class ApproximatedAutoIntervalFactory {
    static final ImmutableRangeMap<Duration, Duration> boundaries = ImmutableRangeMap.<Duration, Duration>builder()
            .put(Range.atMost( Duration.ofMillis(20)), Duration.ofMillis(1))
            .put(Range.openClosed(Duration.ofMillis(20), Duration.ofMillis(200)), Duration.ofMillis(5))
            .put(Range.openClosed(Duration.ofMillis(200), Duration.ofMillis(500)), Duration.ofMillis(10))
            .put(Range.openClosed(Duration.ofMillis(500), Duration.ofMillis(1000)), Duration.ofMillis(20))
            .put(Range.openClosed(Duration.ofMillis(1000), Duration.ofSeconds(2)), Duration.ofMillis(40))
            .put(Range.openClosed(Duration.ofSeconds(2), Duration.ofSeconds(10)), Duration.ofMillis(200))
            .put(Range.openClosed(Duration.ofSeconds(10), Duration.ofSeconds(30)), Duration.ofMillis(500))
            .put(Range.openClosed(Duration.ofSeconds(30), Duration.ofMinutes(1)), Duration.ofSeconds(1))
            .put(Range.openClosed(Duration.ofMinutes(1), Duration.ofMinutes(2)), Duration.ofSeconds(2))
            .put(Range.openClosed(Duration.ofMinutes(2), Duration.ofMinutes(4)), Duration.ofSeconds(5))
            .put(Range.openClosed(Duration.ofMinutes(4), Duration.ofMinutes(7)), Duration.ofSeconds(10))
            .put(Range.openClosed(Duration.ofMinutes(7), Duration.ofMinutes(10)), Duration.ofSeconds(20))
            .put(Range.openClosed(Duration.ofMinutes(10), Duration.ofMinutes(30)), Duration.ofSeconds(30))
            .put(Range.openClosed(Duration.ofMinutes(30), Duration.ofHours(1)), Duration.ofMinutes(1))
            .put(Range.openClosed(Duration.ofHours(1), Duration.ofHours(2)), Duration.ofMinutes(2))
            .put(Range.openClosed(Duration.ofHours(2), Duration.ofHours(4)), Duration.ofMinutes(5))
            .put(Range.openClosed(Duration.ofHours(4), Duration.ofHours(12)), Duration.ofMinutes(10))
            .put(Range.openClosed(Duration.ofHours(12), Duration.ofHours(16)), Duration.ofMinutes(15))
            .put(Range.openClosed(Duration.ofHours(16), Duration.ofDays(1)), Duration.ofMinutes(30))
            .put(Range.openClosed(Duration.ofDays(1), Duration.ofDays(2)), Duration.ofHours(1))
            .put(Range.openClosed(Duration.ofDays(2), Duration.ofDays(4)), Duration.ofHours(2))
            .put(Range.openClosed(Duration.ofDays(4), Duration.ofDays(10)), Duration.ofHours(4))
            .put(Range.openClosed(Duration.ofDays(10), Duration.ofDays(12)), Duration.ofHours(6))
            .put(Range.openClosed(Duration.ofDays(12), Duration.ofDays(28)), Duration.ofHours(12))
            .put(Range.openClosed(Duration.ofDays(28), Duration.ofDays(60)), Duration.ofDays(1))
            .put(Range.openClosed(Duration.ofDays(60), Duration.ofDays(120)), Duration.ofDays(2))
            .put(Range.openClosed(Duration.ofDays(120), Duration.ofDays(180)), Duration.ofDays(3))
            .put(Range.openClosed(Duration.ofDays(180), Duration.ofDays(365)), Duration.ofDays(7))
            .put(Range.open(Duration.ofDays(365), Duration.ofDays(730)), Duration.ofDays(14))
            .put(Range.atLeast(Duration.ofDays(730)), Duration.ofDays(30))
            .build();

    private static Double scalingForAutoTimeRange(long durationOfTimeRangeInSeconds, Duration durationOfTimeRange) {
        final Duration autoIntervalDuration = boundaries.get(Duration.ofSeconds(durationOfTimeRangeInSeconds));
        return (double) autoIntervalDuration.getSeconds() / (double) durationOfTimeRange.getSeconds();
    }

    private static Duration parseInterval(String interval) {
        switch (interval) {
            case "minute": return Duration.ofMinutes(1);
            case "hour": return Duration.ofHours(1);
            case "day": return Duration.ofDays(1);
            case "week": return Duration.ofDays(7);
            case "month": return Duration.ofDays(30);
            case "quarter": return Duration.ofDays(90);
            case "year": return Duration.ofDays(365);
        }
        throw new RuntimeException("Unable to parse interval: " + interval);
    }

    private static Interval timestampInterval(String interval) {
        switch (interval) {
            case "minute": return TimeUnitInterval.create(TimeUnitInterval.IntervalUnit.MINUTES, 1);
            case "hour": return TimeUnitInterval.create(TimeUnitInterval.IntervalUnit.HOURS, 1);
            case "day": return TimeUnitInterval.create(TimeUnitInterval.IntervalUnit.DAYS, 1);
            case "week": return TimeUnitInterval.create(TimeUnitInterval.IntervalUnit.WEEKS, 1);
            case "month": return TimeUnitInterval.create(TimeUnitInterval.IntervalUnit.MONTHS, 1);
            case "quarter": return TimeUnitInterval.create(TimeUnitInterval.IntervalUnit.MONTHS, 3);
            case "year": return TimeUnitInterval.create(TimeUnitInterval.IntervalUnit.YEARS, 1);
        }
        throw new RuntimeException("Unable to map interval: " + interval);
    }

    private static Interval ofAbsoluteRange(String interval, AbsoluteRange absoluteRange) {
        final Duration duration = parseInterval(interval);

        final long absoluteTimeRangeDurationInSeconds = (absoluteRange.to().getMillis() - absoluteRange.from().getMillis()) / 1000;
        final double absoluteScaling = scalingForAutoTimeRange(absoluteTimeRangeDurationInSeconds, duration);
        if (absoluteScaling < 0.5 || absoluteScaling > 8.0) {
            return timestampInterval(interval);
        }
        return AutoInterval.create(absoluteScaling);
    }

    private static Interval ofRelativeRange(String interval, RelativeRange relativeRange) {
        final Duration duration = parseInterval(interval);

        final long relativeTimeRangeDurationInSeconds = relativeRange.range();
        final double relativeScaling = scalingForAutoTimeRange(relativeTimeRangeDurationInSeconds, duration);
        if (relativeScaling < 0.5 || relativeScaling > 8.0) {
            return timestampInterval(interval);
        }
        return AutoInterval.create(relativeScaling);
    }

    public static Interval of(String interval, TimeRange timeRange) {
        switch (timeRange.type()) {
            case TimeRange.KEYWORD: return timestampInterval(interval);
            case TimeRange.ABSOLUTE:
                return ofAbsoluteRange(interval, (AbsoluteRange)timeRange);
            case TimeRange.RELATIVE:
                return ofRelativeRange(interval, (RelativeRange)timeRange);
        }

        throw new RuntimeException("Unable to parse time range type: " + timeRange.type());
    }
}
