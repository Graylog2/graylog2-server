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
package org.graylog2.plugin.utilities.date;

import com.google.common.collect.Maps;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class NaturalDateParser {
    public static final String DEFAULT_TIMEZONE = "UTC";

    public Result parse(final String string) throws DateNotParsableException {
        return parse(string, DEFAULT_TIMEZONE);
    }

    public Result parse(final String string, final String timezone) throws DateNotParsableException {
        Date from = null;
        Date to = null;
        TimeZone tz = getTimezoneFromString(timezone);

        final Parser parser = new Parser(tz);
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

        return new Result(from, to, tz);
    }

    private TimeZone getTimezoneFromString(String timezone) {
        String timezoneString = timezone;
        if (timezoneString.isEmpty()) {
            timezoneString = DEFAULT_TIMEZONE;
        }
        return TimeZone.getTimeZone(timezoneString);
    }

    public static class Result {
        private final DateTime from;
        private final DateTime to;
        private final DateTimeZone timezone;

        public Result(final Date from, final Date to, final TimeZone timezone) {
            this.timezone = DateTimeZone.forTimeZone(timezone);
            if (from != null) {
                this.from = new DateTime(from, this.timezone);
            } else {
                this.from = new DateTime(this.timezone);
            }

            if (to != null) {
                this.to = new DateTime(to, this.timezone);
            } else {
                this.to = new DateTime(this.timezone);
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
            return x.toString(DateTimeFormat.forPattern(Tools.ES_DATE_FORMAT_NO_MS).withZone(this.timezone));
        }
    }

    public static class DateNotParsableException extends Exception {
        public DateNotParsableException(String message) {
            super(message);
        }
    }

}
