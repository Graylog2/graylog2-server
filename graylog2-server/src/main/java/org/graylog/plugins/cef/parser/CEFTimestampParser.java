/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.cef.parser;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.annotation.Nullable;
import java.util.Locale;

public class CEFTimestampParser {
    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = {
            ISODateTimeFormat.dateTime(),
            ISODateTimeFormat.dateTimeNoMillis(),
            ISODateTimeFormat.basicDateTime(),
            ISODateTimeFormat.basicDateTimeNoMillis(),

            DateTimeFormat.forPattern("MMM dd HH:mm:ss.SSS ZZZ"),
            DateTimeFormat.forPattern("MMM dd HH:mm:sss.SSS"),
            DateTimeFormat.forPattern("MMM dd HH:mm:ss ZZZ"),
            DateTimeFormat.forPattern("MMM dd HH:mm:ss"),
            DateTimeFormat.forPattern("MMM dd yyyy HH:mm:ss.SSS ZZZ"),
            DateTimeFormat.forPattern("MMM dd yyyy HH:mm:ss.SSS"),
            DateTimeFormat.forPattern("MMM dd yyyy HH:mm:ss ZZZ"),
            DateTimeFormat.forPattern("MMM dd yyyy HH:mm:ss"),

            DateTimeFormat.fullDateTime(),
            DateTimeFormat.mediumDateTime(),
            DateTimeFormat.shortDateTime(),
            DateTimeFormat.longDateTime()
    };

    @Nullable
    public static DateTime parse(String s, DateTimeZone timeZone, Locale locale) {
        // Trim input string and consolidate repeating blanks
        final String text = s.trim().replaceAll("\\s{2,}", " ");

        // First try UNIX epoch millisecond timestamp
        try {
            final long l = Long.parseLong(text);
            return new DateTime(l, DateTimeZone.UTC);
        } catch (NumberFormatException e) {
            // ignore
        }

        for (DateTimeFormatter dateTimeFormatter : DATE_TIME_FORMATTERS) {
            try {
                return dateTimeFormatter
                        .withZone(timeZone)
                        .withLocale(locale)
                        .parseDateTime(text);
            } catch (Exception e) {
                // ignore
            }
        }
        return null;
    }

    @Nullable
    public static DateTime parse(String text) {
        return parse(text, DateTimeZone.UTC, Locale.ROOT);
    }
}
