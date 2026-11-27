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
import java.util.List;

/**
 * How documents are assigned to buckets, based on the values of a {@link GroupedValueSource}.
 * <p>
 * Documents without a value, or whose values the grouping can't place (e.g. a string in a date histogram), belong
 * to the missing bucket.
 */
public sealed interface Grouping permits Terms, Ranges, DateHistogram {

    GroupedValueSource source();

    /**
     * The same grouping of another source's values.
     */
    Grouping withSource(GroupedValueSource source);

    static Terms terms(final String field) {
        return new Terms(GroupedValueSource.field(field));
    }

    static Terms terms(final GroupedValueSource source) {
        return new Terms(source);
    }

    static Ranges ranges(final GroupedValueSource source, final List<Range> ranges) {
        return new Ranges(source, ranges);
    }

    static DateHistogram dateHistogram(final GroupedValueSource source, final DateUnit unit, final ZoneId timezone) {
        return new DateHistogram(source, unit, 1, timezone);
    }
}
