package org.graylog.testing.utils;

import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;

public final class RangeUtils {
    private RangeUtils(){}

    public static AbsoluteRange allMessagesTimeRange() {
        try {
            return AbsoluteRange.create("2010-01-01T00:00:00.0Z", "2050-01-01T00:00:00.0Z");
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException("boo hoo", e);
        }
    }
}
