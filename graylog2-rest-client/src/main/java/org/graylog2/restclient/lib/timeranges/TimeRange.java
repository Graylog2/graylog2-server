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
