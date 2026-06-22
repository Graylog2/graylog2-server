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
package org.graylog.mcp.tools;

import jakarta.annotation.Nullable;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Shared time range handling for MCP tools that accept either {@code range_seconds} or an absolute {@code from}/{@code to} window.
 */
final class McpTimeRanges {
    private static final DateTimeFormatter ISO_8601 = ISODateTimeFormat.dateTimeParser().withZoneUTC();

    private McpTimeRanges() {}

    static TimeRange timerange(@Nullable String from, @Nullable String to, int rangeSeconds) {
        if (from == null && to == null) {
            return RelativeRange.create(rangeSeconds);
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("Pass both from and to for an absolute time range, or neither");
        }
        return AbsoluteRange.create(parseTimestamp("from", from), parseTimestamp("to", to));
    }

    // Accepts ISO 8601 with or without fractional seconds, which callers often omit.
    private static DateTime parseTimestamp(String name, String value) {
        try {
            return ISO_8601.parseDateTime(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(f("%s must be an ISO 8601 timestamp, got <%s>", name, value), e);
        }
    }
}
