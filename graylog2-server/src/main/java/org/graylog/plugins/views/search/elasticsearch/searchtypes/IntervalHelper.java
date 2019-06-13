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
package org.graylog.plugins.views.search.elasticsearch.searchtypes;

import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.Duration;

public class IntervalHelper {
    public static Searches.DateHistogramInterval createDefaultInterval(final TimeRange timeRange) {
        final Duration duration = Duration.millis(timeRange.getTo().getMillis() - timeRange.getFrom().getMillis());

        // This is the same as SearchPage#_determineHistogramResolution() in the UI code
        if (duration.getStandardHours() < 12) {
            return Searches.DateHistogramInterval.MINUTE;
        } else if (duration.getStandardDays() < 3) {
            return Searches.DateHistogramInterval.HOUR;
        } else if (duration.getStandardDays() < 30) {
            return Searches.DateHistogramInterval.DAY;
        } else if (duration.getStandardDays() < (30 * 2)) {
            return Searches.DateHistogramInterval.WEEK;
        } else if (duration.getStandardDays() < (30 * 18)) {
            return Searches.DateHistogramInterval.MONTH;
        } else if (duration.getStandardDays() < (365 * 3)) {
            return Searches.DateHistogramInterval.QUARTER;
        } else {
            return Searches.DateHistogramInterval.YEAR;
        }
    }
}
