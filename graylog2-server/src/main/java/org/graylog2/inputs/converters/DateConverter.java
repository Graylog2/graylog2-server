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
package org.graylog2.inputs.converters;

import org.graylog2.ConfigurationException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.Converter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.YearMonth;
import org.joda.time.Years;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public class DateConverter extends Converter {
    private static final Logger LOG = LoggerFactory.getLogger(DateConverter.class);

    private final String dateFormat;
    private final DateTimeZone timeZone;

    public DateConverter(Map<String, Object> config) throws ConfigurationException {
        super(Type.DATE, config);

        if (config.get("date_format") == null || ((String) config.get("date_format")).isEmpty()) {
            throw new ConfigurationException("Missing config [date_format].");
        }

        this.dateFormat = ((String) config.get("date_format")).trim();
        this.timeZone = buildTimeZone(config.get("time_zone"));
    }

    private static DateTimeZone buildTimeZone(Object timeZoneId) {
        if (timeZoneId instanceof String) {
            try {
                final String timeZoneString = (String) timeZoneId;
                final String zoneId = firstNonNull(emptyToNull(timeZoneString.trim()), "Etc/UTC");
                return DateTimeZone.forID(zoneId);
            } catch (IllegalArgumentException e) {
                return DateTimeZone.forID("Etc/UTC");
            }
        } else {
            return DateTimeZone.forID("Etc/UTC");
        }
    }

    @Override
    @Nullable
    public Object convert(@Nullable String value) {
        if (isNullOrEmpty(value)) {
            return null;
        }

        LOG.debug("Trying to parse date <{}> with pattern <{}> and timezone <{}>.", value, dateFormat, timeZone);
        final DateTimeFormatter formatter = DateTimeFormat
                .forPattern(dateFormat)
                .withDefaultYear(YearMonth.now(timeZone).getYear())
                .withZone(timeZone);
        return DateTime.parse(value, formatter);
    }

    @Override
    public boolean buildsMultipleFields() {
        return false;
    }
}
