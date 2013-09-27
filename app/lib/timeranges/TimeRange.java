/*
 * Copyright 2013 TORCH UG
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
 */
package lib.timeranges;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class TimeRange {

    public enum Type {
        RELATIVE,
        ABSOLUTE,
        KEYWORD
    }

    public abstract Type getType();
    public abstract Map<String, String> getQueryParams();

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

}
