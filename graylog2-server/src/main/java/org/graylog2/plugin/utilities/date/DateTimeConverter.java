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
package org.graylog2.plugin.utilities.date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import static org.graylog2.plugin.Tools.ES_DATE_FORMAT_FORMATTER;
import static org.graylog2.plugin.Tools.ES_DATE_FORMAT_NO_MS_FORMATTER;
import static org.graylog2.plugin.Tools.ISO_DATE_FORMAT_FORMATTER;
import static org.graylog2.plugin.Tools.ISO_DATE_FORMAT_NO_MS_FORMATTER;

public class DateTimeConverter {
    // The date part "yyyy-MM-dd" is always 10 characters, so index 10 holds the separator
    // between the date and the time component.
    private static final int DATE_TIME_SEPARATOR_INDEX = 10;

    public static DateTime convertToDateTime(@Nonnull Object value) {
        if (value instanceof DateTime) {
            return (DateTime) value;
        }

        if (value instanceof Date) {
            return new DateTime(value, DateTimeZone.UTC);
        } else if (value instanceof ZonedDateTime) {
            final DateTimeZone dateTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone(((ZonedDateTime) value).getZone()));
            return new DateTime(Date.from(((ZonedDateTime) value).toInstant()), dateTimeZone);
        } else if (value instanceof OffsetDateTime) {
            return new DateTime(Date.from(((OffsetDateTime) value).toInstant()), DateTimeZone.UTC);
        } else if (value instanceof LocalDateTime) {
            final LocalDateTime localDateTime = (LocalDateTime) value;
            final ZoneId defaultZoneId = ZoneId.systemDefault();
            final ZoneOffset offset = defaultZoneId.getRules().getOffset(localDateTime);
            return new DateTime(Date.from(localDateTime.toInstant(offset)));
        } else if (value instanceof LocalDate) {
            final LocalDate localDate = (LocalDate) value;
            final LocalDateTime localDateTime = localDate.atStartOfDay();
            final ZoneId defaultZoneId = ZoneId.systemDefault();
            final ZoneOffset offset = defaultZoneId.getRules().getOffset(localDateTime);
            return new DateTime(Date.from(localDateTime.toInstant(offset)));
        } else if (value instanceof Instant) {
            return new DateTime(Date.from((Instant) value), DateTimeZone.UTC);
        } else if (value instanceof String) {
            return parseStringToDateTime((String) value);
        } else {
            throw new IllegalArgumentException("Value of invalid type <" + value.getClass().getSimpleName() + "> provided");
        }
    }

    /**
     * Parses the supported timestamp string formats with a single formatter, selected up front from telltale
     * characters rather than by trying each pattern in turn. All supported strings share the layout
     * {@code yyyy-MM-dd<sep>HH:mm:ss[.SSS][offset]}, where {@code <sep>} is a space (Graylog's internal format) or
     * {@code 'T'} (ISO-8601). The presence of a {@code '.'} selects the millisecond variant.
     *
     * <p>Supported patterns:
     * <ul>
     *     <li>{@code yyyy-MM-dd HH:mm:ss.SSS}</li>
     *     <li>{@code yyyy-MM-dd HH:mm:ss}</li>
     *     <li>{@code yyyy-MM-dd'T'HH:mm:ss.SSSZZ}</li>
     *     <li>{@code yyyy-MM-dd'T'HH:mm:ssZZ}</li>
     * </ul>
     *
     * <p>Anything else (including ISO values without a zone/offset, which are ambiguous) fails fast with an
     * {@link IllegalArgumentException}. See https://github.com/Graylog2/graylog2-server/issues/26025
     */
    private static DateTime parseStringToDateTime(String value) {
        final char separator = value.length() > DATE_TIME_SEPARATOR_INDEX ? value.charAt(DATE_TIME_SEPARATOR_INDEX) : 0;
        final boolean hasMillis = value.indexOf('.') > 0;

        final DateTimeFormatter formatter = switch (separator) {
            case ' ' -> hasMillis ? ES_DATE_FORMAT_FORMATTER : ES_DATE_FORMAT_NO_MS_FORMATTER;
            case 'T' -> hasMillis ? ISO_DATE_FORMAT_FORMATTER : ISO_DATE_FORMAT_NO_MS_FORMATTER;
            default -> throw new IllegalArgumentException("Unsupported timestamp string format: <" + value + ">");
        };
        return formatter.parseDateTime(value);
    }
}
