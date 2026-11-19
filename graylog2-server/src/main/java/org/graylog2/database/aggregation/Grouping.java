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
package org.graylog2.database.aggregation;

import jakarta.annotation.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * How documents are assigned to buckets, based on the values of a {@link ValueSource}.
 * <p>
 * Documents without a value, or whose values the grouping can't place (e.g. a string in a date histogram), belong
 * to the missing bucket.
 */
public sealed interface Grouping {

    ValueSource source();

    /**
     * The same grouping of another source's values.
     */
    Grouping withSource(ValueSource source);

    static Terms terms(final String field) {
        return new Terms(ValueSource.field(field));
    }

    static Terms terms(final ValueSource source) {
        return new Terms(source);
    }

    static Ranges ranges(final ValueSource source, final List<Range> ranges) {
        return new Ranges(source, ranges);
    }

    static DateHistogram dateHistogram(final ValueSource source, final DateUnit unit, final ZoneId timezone) {
        return new DateHistogram(source, unit, 1, timezone);
    }

    /**
     * One bucket per distinct value.
     */
    record Terms(ValueSource source) implements Grouping {
        public Terms {
            Objects.requireNonNull(source, "source");
        }

        @Override
        public Terms withSource(final ValueSource source) {
            return new Terms(source);
        }
    }

    /**
     * One bucket per range, keyed by the range's key. A value belongs to the first range containing it.
     */
    record Ranges(ValueSource source, List<Range> ranges) implements Grouping {
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
        public Ranges withSource(final ValueSource source) {
            return new Ranges(source, ranges);
        }

        public boolean isTemporal() {
            return ranges.getFirst().isTemporal();
        }
    }

    /**
     * A range of numbers or instants, including {@code from} and excluding {@code to}. Either end may be open.
     */
    record Range(String key, @Nullable Object from, @Nullable Object to) {
        public Range {
            Objects.requireNonNull(key, "key");
            if (from == null && to == null) {
                throw new IllegalArgumentException(f("Range <%s> needs at least one bound", key));
            }
            requireBound(key, from);
            requireBound(key, to);
            if (from != null && to != null) {
                if (from instanceof Instant != to instanceof Instant) {
                    throw new IllegalArgumentException(f("Range <%s> must not mix numbers and instants", key));
                }
                if (compare(from, to) >= 0) {
                    throw new IllegalArgumentException(f("Range <%s> must start before it ends", key));
                }
            }
        }

        public static Range of(final String key, @Nullable final Number from, @Nullable final Number to) {
            return new Range(key, from, to);
        }

        public static Range of(final String key, @Nullable final Instant from, @Nullable final Instant to) {
            return new Range(key, from, to);
        }

        public boolean isTemporal() {
            return (from != null ? from : to) instanceof Instant;
        }

        private static void requireBound(final String key, @Nullable final Object bound) {
            if (bound != null && !(bound instanceof Number) && !(bound instanceof Instant)) {
                throw new IllegalArgumentException(f("Range <%s> bounds must be numbers or instants", key));
            }
        }

        private static int compare(final Object from, final Object to) {
            if (from instanceof Instant fromInstant) {
                return fromInstant.compareTo((Instant) to);
            }
            return Double.compare(((Number) from).doubleValue(), ((Number) to).doubleValue());
        }
    }

    /**
     * One bucket per period that has values, keyed by the period's start. Periods are aligned to the calendar in
     * {@code timezone}; weeks start on Monday.
     */
    record DateHistogram(ValueSource source, DateUnit unit, int binSize, ZoneId timezone) implements Grouping {
        public DateHistogram {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(unit, "unit");
            Objects.requireNonNull(timezone, "timezone");
            if (binSize < 1) {
                throw new IllegalArgumentException("Bin size must be at least 1");
            }
        }

        @Override
        public DateHistogram withSource(final ValueSource source) {
            return new DateHistogram(source, unit, binSize, timezone);
        }

        public DateHistogram withBinSize(final int binSize) {
            return new DateHistogram(source, unit, binSize, timezone);
        }
    }

    enum DateUnit {
        YEAR, QUARTER, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND;

        String mongoName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
