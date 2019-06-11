package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.search.Query;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * Dummy class to allow constructing an empty {@link Query query instance}.
 */
public class EmptyTimeRange extends TimeRange {

    private static final EmptyTimeRange INSTANCE = new EmptyTimeRange();

    @Override
    public String type() {
        return "empty";
    }

    @Override
    public DateTime getFrom() {
        return null;
    }

    @Override
    public DateTime getTo() {
        return null;
    }

    @Override
    public Map<String, Object> getPersistedConfig() {
        return null;
    }

    public static TimeRange emptyTimeRange() {
        return INSTANCE;
    }
}
