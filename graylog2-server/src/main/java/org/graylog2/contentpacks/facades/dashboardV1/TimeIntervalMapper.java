package org.graylog2.contentpacks.facades.dashboardV1;


import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.TimeUnitInterval;

import java.util.Map;

public class TimeIntervalMapper {
    private static final Map<String, TimeUnitInterval> intervalMap = new ImmutableMap.Builder<String, TimeUnitInterval>()
            .put("minute", TimeUnitInterval.builder().timeunit("1m").build())
            .put("hour", TimeUnitInterval.builder().timeunit("1h").build())
            .put("day", TimeUnitInterval.builder().timeunit("1d").build())
            .put("week", TimeUnitInterval.builder().timeunit("1w").build())
            .put("month", TimeUnitInterval.builder().timeunit("1M").build())
            .put("quarter", TimeUnitInterval.builder().timeunit("3M").build())
            .put("year", TimeUnitInterval.builder().timeunit("1y").build())
            .build();

    public static TimeUnitInterval map(String unit) {
        return intervalMap.getOrDefault(unit, TimeUnitInterval.builder().timeunit("1m").build());
    }
}
