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
package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.search.Query;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;

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

    public static TimeRange emptyTimeRange() {
        return INSTANCE;
    }
}
