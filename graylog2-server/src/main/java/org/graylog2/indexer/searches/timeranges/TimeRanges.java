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
package org.graylog2.indexer.searches.timeranges;

import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.Seconds;

public final class TimeRanges {
    private TimeRanges() {
    }

    /**
     * Calculate the number of seconds in the given time range.
     *
     * @param timeRange the {@link TimeRange}
     * @return the number of seconds in the given time range or 0 if an error occurred.
     */
    public static int toSeconds(TimeRange timeRange) {
        if (timeRange.getFrom() == null || timeRange.getTo() == null) {
            return 0;
        }

        try {
            return Seconds.secondsBetween(timeRange.getFrom(), timeRange.getTo()).getSeconds();
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }
}
