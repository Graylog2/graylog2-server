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

import com.atlassian.ip.IPMatcher;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.InetAddress;
import java.util.Objects;

public final class BlacklistIpMatcherCondition extends FilterDescription {
    private IPMatcher ipMatcher;

    @JsonProperty
    public void setPattern(String pattern) {
        this.pattern = pattern;
        ipMatcher = IPMatcher.builder().addPatternOrHost(pattern).build();
    }

    public boolean matchesInetAddress(InetAddress otherSource) {
        try {
            return ipMatcher.matches(otherSource);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlacklistIpMatcherCondition)) return false;

        BlacklistIpMatcherCondition that = (BlacklistIpMatcherCondition) o;

        return Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern);
    }
}
