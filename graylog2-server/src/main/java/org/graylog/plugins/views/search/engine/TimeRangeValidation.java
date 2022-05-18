package org.graylog.plugins.views.search.engine;

import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.Period;

class TimeRangeValidation {
    static boolean isOutOfLimit(TimeRange timeRange, Period limit) {
        final DateTime start = timeRange.getFrom();
        final DateTime end = timeRange.getTo();
        final DateTime allowedStart = end.minus(limit);
        return start.isBefore(allowedStart);
    }

}
