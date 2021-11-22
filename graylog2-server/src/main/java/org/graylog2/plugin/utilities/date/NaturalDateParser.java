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

import com.google.common.collect.Maps;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class NaturalDateParser {
    private final TimeZone timeZone;
    private final ZoneId zoneId;
    private final DateTimeZone dateTimeZone;
    private final Locale locale;

    public NaturalDateParser() {
        this("Etc/UTC", Locale.getDefault());
    }

    public NaturalDateParser(final Locale locale) {
        this("Etc/UTC", locale);
    }

    public NaturalDateParser(final String timeZone) throws IllegalArgumentException {
        this(timeZone, Locale.getDefault());
    }

    public NaturalDateParser(final String timeZone, final Locale locale) throws IllegalArgumentException {
        if (!isValidTimeZone(timeZone)) {
            throw new IllegalArgumentException("Invalid timeZone: " + timeZone);
        }

        this.locale = locale;
        this.timeZone = TimeZone.getTimeZone(timeZone);
        this.zoneId = ZoneId.of(timeZone);
        this.dateTimeZone = DateTimeZone.forTimeZone(this.timeZone);
    }

    boolean isValidTimeZone(final String timeZone) {
        return Arrays.stream(TimeZone.getAvailableIDs()).anyMatch(id -> id.equals(timeZone));
    }

    boolean matches(String string, String other) {
        return string.equalsIgnoreCase(other.replaceAll("\\s", ""));
    }

    Date alignToStartOf(final Date dateToConvert, final String string) {
        if (matches("lastyear", string) || matches("thisyear", string)) {
            return alignToStartOfYear(dateToConvert);
        } else if (matches("lastmonth", string) || matches("thismonth", string)) {
            return alignToStartOfMonth(dateToConvert);
        } else if (matches("lastweek", string) || matches("thisweek", string)) {
            return alignToStartOfWeek(dateToConvert);
        } else {
            return alignToStartOfDay(dateToConvert);
        }
    }

    Date alignToEndOf(final Date dateToConvert, final String string) {
        if (matches("lastyear", string) || matches("thisyear", string)) {
            return alignToEndOfYear(dateToConvert);
        } else if (matches("lastmonth", string) || matches("thismonth", string)) {
            return alignToEndOfMonth(dateToConvert);
        } else if (matches("lastweek", string) || matches("thisweek", string)) {
            return alignToEndOfWeek(dateToConvert);
        } else {
            return alignToEndOfDay(dateToConvert);
        }
    }

    Date alignToStartOfYear(Date dateToConvert) {
        return java.util.Date.from(dateToConvert.toInstant().atZone(zoneId).toLocalDate().atStartOfDay().atZone(zoneId).withDayOfYear(1).toInstant());
    }

    Date alignToEndOfYear(Date dateToConvert) {
        return java.util.Date.from(dateToConvert.toInstant().atZone(zoneId).toLocalDate().atStartOfDay().atZone(zoneId).withDayOfYear(1).plusYears(1).toInstant());
    }

    Date alignToStartOfMonth(Date dateToConvert) {
        return java.util.Date.from(dateToConvert.toInstant().atZone(zoneId).toLocalDate().atStartOfDay().atZone(zoneId).withDayOfMonth(1).toInstant());
    }

    Date alignToEndOfMonth(Date dateToConvert) {
        return java.util.Date.from(dateToConvert.toInstant().atZone(zoneId).toLocalDate().atStartOfDay().atZone(zoneId).withDayOfMonth(1).plusMonths(1).toInstant());
    }

    Date alignToStartOfWeek(Date dateToConvert) {
        return java.util.Date.from(dateToConvert.toInstant().atZone(zoneId).toLocalDate().atStartOfDay().atZone(zoneId).with(WeekFields.of(this.locale).dayOfWeek(), 1L).toInstant());
    }

    Date alignToEndOfWeek(Date dateToConvert) {
        return java.util.Date.from(dateToConvert.toInstant().atZone(zoneId).toLocalDate().atStartOfDay().atZone(zoneId).with(WeekFields.of(this.locale).dayOfWeek(), 7L).plusDays(1).toInstant());
    }

    Date alignToStartOfDay(Date dateToConvert) {
        return java.util.Date.from(dateToConvert.toInstant().atZone(zoneId).toLocalDate().atStartOfDay().atZone(zoneId).toInstant());
    }

    Date alignToEndOfDay(Date dateToConvert) {
        return java.util.Date.from(dateToConvert.toInstant().atZone(zoneId).plusDays(1).toLocalDate().atStartOfDay().atZone(zoneId).toInstant());
    }

    public Result parse(final String string) throws DateNotParsableException {
        return this.parse(string, new Date());
    }

    Result parse(final String string, final Date referenceDate) throws DateNotParsableException {
        Date from = null;
        Date to = null;

        final Parser parser = new Parser(this.timeZone);
        final List<DateGroup> groups = parser.parse(string, referenceDate);
        if (!groups.isEmpty()) {
            // only working on with the first DateGroup
            final DateGroup group = groups.get(0);
            final boolean timeIsInferred = group.isTimeInferred();

            final List<Date> dates = group.getDates();
            Collections.sort(dates);

            if (dates.size() >= 1) {
                from = timeIsInferred ? alignToStartOf(dates.get(0), string) : dates.get(0);
            }

            if (dates.size() >= 2) {
                to = timeIsInferred ? alignToEndOf(dates.get(1), string) : dates.get(1);
            } else {
                to = timeIsInferred ? alignToEndOf(dates.get(0), string) : null;
            }
        } else {
            throw new DateNotParsableException("Unparsable date: " + string);
        }

        return new Result(from, to, this.dateTimeZone);
    }

    public static class Result {
        private final DateTime from;
        private final DateTime to;
        private final DateTimeZone dateTimeZone;

        public Result(final Date from, final Date to, final DateTimeZone dateTimeZone) {
            this.dateTimeZone = dateTimeZone;

            if (from != null) {
                this.from = new DateTime(from, this.dateTimeZone);
            } else {
                this.from = Tools.now(this.dateTimeZone);
            }

            if (to != null) {
                this.to = new DateTime(to, this.dateTimeZone);
            } else {
                this.to = Tools.now(this.dateTimeZone);
            }
        }

        public DateTime getFrom() {
            return from;
        }

        public DateTime getTo() {
            return to;
        }

        public DateTimeZone getDateTimeZone() {
            return dateTimeZone;
        }

        public Map<String, String> asMap() {
            Map<String, String> result = Maps.newHashMap();

            result.put("from", dateFormat(getFrom()));
            result.put("to", dateFormat(getTo()));
            result.put("timezone", getDateTimeZone().getID());

            return result;
        }

        private String dateFormat(final DateTime x) {
            return x.toString(Tools.ES_DATE_FORMAT_NO_MS_FORMATTER.withZone(dateTimeZone));
        }
    }

    public static class DateNotParsableException extends Exception {
        public DateNotParsableException(String message) {
            super(message);
        }
    }

}
