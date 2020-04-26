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
package org.graylog2.timeranges;

import com.google.common.base.Strings;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Map;

public class TimeRangeFactory {
    public TimeRange create(final Map<String, Object> timerangeConfig) throws InvalidRangeParametersException {
        final String rangeType = Strings.isNullOrEmpty((String)timerangeConfig.get("type")) ? (String)timerangeConfig.get("range_type") : (String)timerangeConfig.get("type");
        if (Strings.isNullOrEmpty(rangeType)) {
            throw new InvalidRangeParametersException("range type not set");
        }
        switch (rangeType) {
            case "relative":
                return RelativeRange.create(Integer.parseInt(String.valueOf(timerangeConfig.get("range"))));
            case "keyword":
                return KeywordRange.create((String) timerangeConfig.get("keyword"), (String) timerangeConfig.get("timezone"));
            case "absolute":
                final String from = new DateTime(timerangeConfig.get("from"), DateTimeZone.UTC).toString();
                final String to = new DateTime(timerangeConfig.get("to"), DateTimeZone.UTC).toString();

                return AbsoluteRange.create(from, to);
            default:
                throw new InvalidRangeParametersException("range_type not recognized");
        }
    }
}
