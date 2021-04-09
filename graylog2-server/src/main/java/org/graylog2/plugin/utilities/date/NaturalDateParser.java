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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class NaturalDateParser {
    private final TimeZone timeZone;
    private final DateTimeZone dateTimeZone;

    public NaturalDateParser() {
        this("Etc/UTC");
    }

    public NaturalDateParser(final String timeZone) throws IllegalArgumentException {
        if(!isValidTimeZone(timeZone))
            throw new IllegalArgumentException(String.format("TimeZone %s is not a valid TimeZone", timeZone));

        this.timeZone = TimeZone.getTimeZone(timeZone);
        this.dateTimeZone = DateTimeZone.forTimeZone(this.timeZone);
    }

    boolean isValidTimeZone(final String timeZone) {
        return Arrays.stream(TimeZone.getAvailableIDs()).anyMatch(id -> id.equals(timeZone));
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
            final List<Date> dates = groups.get(0).getDates();
            Collections.sort(dates);

            if (dates.size() >= 1) {
                from = dates.get(0);
            }

            if (dates.size() >= 2) {
                to = dates.get(1);
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
