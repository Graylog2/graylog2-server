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
package org.graylog2.filters.blacklist;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.regex.Pattern;

public final class BlacklistPatternCondition extends FilterDescription {

    private Pattern regex;

    public BlacklistPatternCondition() {
    }

    @JsonProperty
    public void setPattern(String pattern) {
        this.pattern = pattern;
        this.regex = Pattern.compile(pattern);
    }

    public boolean matchesPattern(Object value) {
        return regex.matcher(String.valueOf(value)).matches();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlacklistPatternCondition)) return false;

        BlacklistPatternCondition that = (BlacklistPatternCondition) o;

        return Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern);
    }
}
