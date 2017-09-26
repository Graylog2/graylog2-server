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
