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
package org.graylog2.restclient.lib;

import com.google.common.collect.TreeMultimap;
import org.graylog2.restclient.models.User;
import org.graylog2.restclient.models.UserService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Set;

public class DateTools {

    public static final String ES_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String ES_DATE_FORMAT_NO_MS = "yyyy-MM-dd HH:mm:ss";

    public static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormat.forPattern("E MMM dd YYYY HH:mm:ss.SSS ZZ");
    public static final DateTimeFormatter SHORT_DATE_FORMAT_TZ = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS ZZ");
    public static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static DateTimeZone globalTimezone = DateTimeZone.UTC;

    private static final TreeMultimap<String, String> groupedZones;

    static {
        groupedZones = TreeMultimap.create();

        final Set<String> zones = DateTimeZone.getAvailableIDs();
        for (String zone : zones) {
            // skip "simple" names, we only want descriptive names
            if (!zone.contains("/")) {
                continue;
            }
            final String[] groupAndZone = zone.split("/", 2);
            groupedZones.put(groupAndZone[0], groupAndZone[1]);
        }
    }

    public static TreeMultimap<String, String> getGroupedTimezoneIds() {
        return groupedZones;
    }

    public static DateTimeZone getApplicationTimeZone() {
        return globalTimezone;
    }

    public static void setApplicationTimeZone(DateTimeZone tz) {
        globalTimezone = tz;
    }

    public static DateTime inUserTimeZone(DateTime timestamp) {
        DateTimeZone tz = globalTimezone;
        final User currentUser = UserService.current();
        if (currentUser != null && currentUser.getTimeZone() != null) {
            tz = currentUser.getTimeZone();
        }
        return timestamp.toDateTime(tz);
    }

    public static String inUserTimeZoneShortFormat(DateTime esDate) {
        final DateTime timestamp = inUserTimeZone(esDate);
        return timestamp.toString(SHORT_DATE_FORMAT);
    }

    public static DateTime nowInUTC() {
        return DateTime.now(DateTimeZone.UTC);
    }

    public static int getUserTimeZoneOffset(User currentUser) {
        DateTimeZone tz = globalTimezone;

        if (currentUser != null && currentUser.getTimeZone() != null) {
            tz = currentUser.getTimeZone();
        }

        int offset = tz.getOffset(new DateTime().getMillis());

        return ((offset / 1000) / 60) * -1;
    }

    public static String getUserTimeZone(User currentUser) {
        DateTimeZone tz = globalTimezone;

        if (currentUser != null && currentUser.getTimeZone() != null) {
            tz = currentUser.getTimeZone();
        }

        return tz.toString();
    }
}
