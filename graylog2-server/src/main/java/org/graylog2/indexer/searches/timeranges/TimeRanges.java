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
