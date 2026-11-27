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
package org.graylog2.database.aggregation.grouping;

import java.time.ZoneId;
import java.util.Objects;

/**
 * One bucket per period that has values, keyed by the period's start. Periods are aligned to the calendar in
 * {@code timezone}; weeks start on Monday.
 */
public record DateHistogram(GroupedValueSource source, DateUnit unit, int binSize, ZoneId timezone)
        implements Grouping {

    public DateHistogram {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(timezone, "timezone");
        if (binSize < 1) {
            throw new IllegalArgumentException("Bin size must be at least 1");
        }
    }

    @Override
    public DateHistogram withSource(final GroupedValueSource source) {
        return new DateHistogram(source, unit, binSize, timezone);
    }

    public DateHistogram withBinSize(final int binSize) {
        return new DateHistogram(source, unit, binSize, timezone);
    }
}
