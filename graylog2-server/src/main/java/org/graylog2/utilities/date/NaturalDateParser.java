/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.utilities.date;

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
    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    public Result parse(final String string) throws DateNotParsableException {
        Date from = null;
        Date to = null;

        final Parser parser = new Parser(UTC);
        final List<DateGroup> groups = parser.parse(string);
        if (!groups.isEmpty()) {
            final List<Date> dates = groups.get(0).getDates();

            if (dates.size() >= 1) {
                from = dates.get(0);
            }

            if (dates.size() >= 2) {
                to = dates.get(1);
            }

            if(from != null && to != null && from.compareTo(to) > 0) {
                final Date swap = from;
                from = to;
                to = swap;
            }
        } else {
            throw new DateNotParsableException();
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
                this.from = Tools.iso8601();
            }

            if (to != null) {
                this.to = new DateTime(to, DateTimeZone.UTC);
            } else {
                this.to = Tools.iso8601();
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
            return x.toString(DateTimeFormat.forPattern(Tools.ES_DATE_FORMAT_NO_MS).withZoneUTC());
        }
    }

    public static class DateNotParsableException extends Exception {
    }

}
