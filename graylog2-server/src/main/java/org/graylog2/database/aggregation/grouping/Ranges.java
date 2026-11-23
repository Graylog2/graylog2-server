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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * One bucket per range, keyed by the range's key. A value belongs to the first range containing it.
 */
public record Ranges(GroupedValueSource source, List<Range> ranges) implements Grouping {
    public Ranges {
        Objects.requireNonNull(source, "source");
        ranges = List.copyOf(ranges);
        if (ranges.isEmpty()) {
            throw new IllegalArgumentException("At least one range is required");
        }
        final var keys = new HashSet<String>();
        for (final Range range : ranges) {
            if (!keys.add(range.key())) {
                throw new IllegalArgumentException(f("Duplicate range key <%s>", range.key()));
            }
            if (range.isTemporal() != ranges.getFirst().isTemporal()) {
                throw new IllegalArgumentException("Ranges must not mix numbers and instants");
            }
        }
    }

    @Override
    public Ranges withSource(final GroupedValueSource source) {
        return new Ranges(source, ranges);
    }

    public boolean isTemporal() {
        return ranges.getFirst().isTemporal();
    }
}
