/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class DateTools {

    public static final String ES_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String ES_DATE_FORMAT_NO_MS = "yyyy-MM-dd HH:mm:ss";

    public static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormat.forPattern("E MMM dd YYYY HH:mm:ss.SSS ZZ");
    public static final DateTimeFormatter SHORT_DATE_FORMAT_TZ = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS ZZ");
    public static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static DateTimeZone globalTimezone = DateTimeZone.getDefault();

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

    public static int getUserTimeZoneOffset() {
        DateTimeZone tz = globalTimezone;

        final User currentUser = UserService.current();

        if (currentUser != null && currentUser.getTimeZone() != null) {
            tz = currentUser.getTimeZone();
        }

        int offsetMillis = tz.toTimeZone().getRawOffset() / 1000;

        if (tz.toTimeZone().useDaylightTime()) {
            offsetMillis += 3600;
        }

        return (offsetMillis / 60) * -1;
    }
}
