/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.utilities.date;

import com.google.common.collect.Maps;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import java.util.*;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class NaturalDateParser {

    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    public Result parse(String string) throws DateNotParsableException {
        Date from = null;
        Date to = null;

        Parser parser = new Parser(UTC);
        List<DateGroup> groups = parser.parse(string);
        if (!groups.isEmpty()) {
            List<Date> dates = groups.get(0).getDates();

            if (dates.size() >= 1) {
                from = dates.get(0);
            }

            if (dates.size() >= 2) {
                to = dates.get(1);
            }
        } else {
            throw new DateNotParsableException();
        }

        return new Result(from, to);
    }

    public class Result {

        private DateTime from;
        private DateTime to;

        public Result(Date from, Date to) {
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

        private String dateFormat(DateTime x) {
            return x.toString(DateTimeFormat.forPattern(Tools.ES_DATE_FORMAT_NO_MS).withZoneUTC());
        }

    }

    public class DateNotParsableException extends Throwable {
    }

}
