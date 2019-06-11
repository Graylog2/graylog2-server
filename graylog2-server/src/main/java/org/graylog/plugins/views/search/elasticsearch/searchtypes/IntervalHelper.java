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
