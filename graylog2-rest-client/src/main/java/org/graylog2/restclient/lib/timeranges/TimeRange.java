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
package org.graylog2.restclient.lib.timeranges;

import java.util.Map;

public abstract class TimeRange {

    public enum Type {
        RELATIVE,
        ABSOLUTE,
        KEYWORD
    }

    public abstract Type getType();
    public abstract Map<String, String> getQueryParams();

    public abstract String toString();

    public String nullSafeParam(String key) {
        if (getQueryParams().containsKey(key)) {
            return getQueryParams().get(key);
        }

        return "";
    }

    public static TimeRange factory(String rangeType, int relative, String from, String to, String keyword) throws InvalidRangeParametersException {
        switch (Type.valueOf(rangeType.toUpperCase())) {
            case RELATIVE:
                return new RelativeRange(relative);
            case ABSOLUTE:
                return new AbsoluteRange(from, to);
            case KEYWORD:
                return new KeywordRange(keyword);
            default:
                throw new InvalidRangeParametersException();
        }
    }

    /**
     * Builds a timerange from Maps like those, coming from API responses:
     *
     * {
     *   "range": 3600,
     *   "type": "relative"
     * }
     *
     * {
     *   "to": "2013-11-05T16:01:02.000+0000",
     *   "from": "2013-11-05T15:42:17.000+0000",
     *   "type": "absolute"
     * }
     *
     * "timerange": {
     *   "keyword": "november 5h last year",
     *   "type": "keyword"
     * }
     *
     * @param timerangeConfig
     * @return
     * @throws InvalidRangeParametersException
     */
    public static TimeRange factory(Map<String, Object> timerangeConfig) throws InvalidRangeParametersException {
        String rangeType = (String) timerangeConfig.get("type");

        switch (Type.valueOf(rangeType.toUpperCase())) {
            case RELATIVE:
                return new RelativeRange(((Number) timerangeConfig.get("range")).intValue());
            case ABSOLUTE:
                return new AbsoluteRange((String) timerangeConfig.get("from"), (String) timerangeConfig.get("to"));
            case KEYWORD:
                return new KeywordRange((String) timerangeConfig.get("keyword"));
            default:
                throw new InvalidRangeParametersException();
        }
    }

}
