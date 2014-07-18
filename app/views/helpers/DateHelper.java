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
package views.helpers;

import org.graylog2.restclient.lib.DateTools;
import org.joda.time.DateTime;
import play.api.templates.Html;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class DateHelper {

    public static Html current() {
        return timestamp(DateTime.now());
    }

    public static Html timestamp(DateTime instant) {
        return views.html.partials.dates.instant.render(DateTools.inUserTimeZone(instant), DateTools.DEFAULT_DATE_FORMAT);
    }

    public static Html timestampShort(DateTime instant) {
        return views.html.partials.dates.instant.render(DateTools.inUserTimeZone(instant), DateTools.SHORT_DATE_FORMAT);
    }

    public static Html timestampShortTZ(DateTime instant) {
        return timestampShortTZ(instant, true);
    }

    public static Html timestampShortTZ(DateTime instant, boolean inUserTZ) {
        DateTime date = instant;

        if (inUserTZ) {
            date = DateTools.inUserTimeZone(instant);
        }
        return views.html.partials.dates.instant.render(date, DateTools.SHORT_DATE_FORMAT_TZ);
    }

    public static Html readablePeriodFromNow(DateTime instant) {
        return readablePeriodFromNow(instant, "");
    }

    public static Html readablePeriodFromNow(DateTime instant, String classes) {
        return views.html.partials.dates.readable_period.render(DateTools.inUserTimeZone(instant), classes);
    }

}
