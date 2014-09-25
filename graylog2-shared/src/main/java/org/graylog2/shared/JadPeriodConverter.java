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
package org.graylog2.shared;

import com.github.joschi.jadconfig.Converter;
import com.github.joschi.jadconfig.ParameterException;
import org.joda.time.Period;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allow durations to be passed in as number + unit or as a IsoPeriodFormat (standard, without millis).
 * See {@link org.joda.time.format.ISOPeriodFormat}
 *
 * 23s -> Period.seconds(23)
 * 30m -> Period.minutes(30)
 * 30h -> Period.hours(30)
 * 30d -> Period.days(30)
 */
public class JadPeriodConverter implements Converter<Period> {
    @Override
    public Period convertFrom(String value) {
        final String trimmedValue = value.trim();
        Period period = null;

        if (trimmedValue.startsWith("P")) {
            // pure IsoPeriod format, try to parse it directly
            period = Period.parse(trimmedValue);
        } else {
            final Matcher matcher = Pattern.compile("(\\d+?)\\s*([s|m|h|d])").matcher(trimmedValue);
            if (matcher.matches()) {
                final long duration = Long.valueOf(matcher.group(1));

                StringBuilder asIsoFormat = new StringBuilder();
                asIsoFormat.append('P');

                final String unit = matcher.group(2);
                switch (unit) {
                    // time units, format must be prefixed with 'T'
                    case "s":
                    case "m":
                    case "h":
                        asIsoFormat.append('T');
                        break;
                    case "d":
                        // no prefix necessary
                        break;
                    default:
                        throw new ParameterException("Couldn't convert value \"" + value + "\" to Period object, invalid unit.");
                }
                asIsoFormat.append(duration).append(unit.toUpperCase());
                period = Period.parse(asIsoFormat.toString());
            }
        }
        if (period == null) {
            throw new ParameterException("Couldn't convert value \"" + value + "\" to Period object.");
        }
        return period;
    }

    @Override
    public String convertTo(Period value) {
        return value.toString();
    }
}
