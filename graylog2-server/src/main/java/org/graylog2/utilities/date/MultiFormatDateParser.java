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
package org.graylog2.utilities.date;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.List;

import static org.graylog2.plugin.Tools.ES_DATE_FORMAT_FORMATTER;
import static org.graylog2.plugin.Tools.ES_DATE_FORMAT_NO_MS_FORMATTER;

public record MultiFormatDateParser(List<DateTimeFormatter> dateTimeFormatters) {

    // We parse all date strings in UTC because we store and show all dates in UTC as well.
    private static final List<DateTimeFormatter> DEFAULT_DATE_TIME_FORMATTERS = ImmutableList.of(
            ES_DATE_FORMAT_NO_MS_FORMATTER,
            ES_DATE_FORMAT_FORMATTER,
            ISODateTimeFormat.dateTimeParser().withOffsetParsed().withZoneUTC()
    );

    public MultiFormatDateParser(final List<DateTimeFormatter> dateTimeFormatters) {
        this.dateTimeFormatters = dateTimeFormatters;
    }

    public MultiFormatDateParser() {
        this(DEFAULT_DATE_TIME_FORMATTERS);
    }

    public DateTime parseDate(final String value) {
        for (DateTimeFormatter formatter : dateTimeFormatters) {
            try {
                return formatter.parseDateTime(value);
            } catch (Exception e) {
                // Try next one
            }
        }

        // It's probably not a date...
        throw new IllegalArgumentException("Unable to parse date: " + value);
    }
}
