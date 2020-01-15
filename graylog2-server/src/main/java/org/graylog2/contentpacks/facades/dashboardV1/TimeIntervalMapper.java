/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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
