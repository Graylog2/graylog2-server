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

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class KeywordRange extends TimeRange {

    private final String keyword;

    public KeywordRange(String keyword) throws InvalidRangeParametersException {
        if (isNullOrEmpty(keyword)) {
            throw new InvalidRangeParametersException();
        }

        this.keyword = keyword;
    }

    @Override
    public TimeRange.Type getType() {
        return Type.KEYWORD;
    }

    @Override
    public Map<String, String> getQueryParams() {
        return ImmutableMap.of(
                "range_type", getType().toString().toLowerCase(),
                "keyword", String.valueOf(keyword));
    }

    @Override
    public String toString() {
        return "Keyword time range [" + getClass().getCanonicalName() + "] - keyword: " + keyword;
    }

}
