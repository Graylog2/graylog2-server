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
package org.graylog2.filters.blacklist;

import com.atlassian.ip.IPMatcher;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class BlacklistIpMatcherCondition extends FilterDescription {
    private static final Logger LOG = LoggerFactory.getLogger(BlacklistIpMatcherCondition.class);

    private IPMatcher ipMatcher;

    public BlacklistIpMatcherCondition() {
    }

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

        if (!pattern.equals(that.pattern)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return pattern.hashCode();
    }
}
