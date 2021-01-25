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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class NaturalDateParser {
    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    public Result parse(final String string) throws DateNotParsableException {
        Date from = null;
        Date to = null;

        final Parser parser = new Parser(UTC);
        final List<DateGroup> groups = parser.parse(string);
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

        return new Result(from, to);
    }

    public static class Result {
        private final DateTime from;
        private final DateTime to;

        public Result(final Date from, final Date to) {
            if (from != null) {
                this.from = new DateTime(from, DateTimeZone.UTC);
            } else {
                this.from = Tools.nowUTC();
            }

            if (to != null) {
                this.to = new DateTime(to, DateTimeZone.UTC);
            } else {
                this.to = Tools.nowUTC();
            }
        }

        public DateTime getFrom() {
            return from;
        }

        public DateTime getTo() {
            return to;
        }

        public Map<String, String> asMap() {
            Map<String, String> result = Maps.newHashMap();

            result.put("from", dateFormat(getFrom()));
            result.put("to", dateFormat(getTo()));

            return result;
        }

        private String dateFormat(final DateTime x) {
            return x.toString(Tools.ES_DATE_FORMAT_NO_MS_FORMATTER.withZoneUTC());
        }
    }

    public static class DateNotParsableException extends Exception {
        public DateNotParsableException(String message) {
            super(message);
        }
    }

}
