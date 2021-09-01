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
