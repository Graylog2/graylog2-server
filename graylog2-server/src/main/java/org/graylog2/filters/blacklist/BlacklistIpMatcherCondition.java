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
import org.graylog2.utilities.IpSubnet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public final class BlacklistIpMatcherCondition extends FilterDescription {
    private IpSubnet ipSubnet;

    @JsonProperty
    public void setPattern(String pattern) {
        this.pattern = pattern;
        try {
            this.ipSubnet = new IpSubnet(pattern);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IP subnet pattern", e);
        }
    }

    public boolean matchesInetAddress(InetAddress otherSource) {
        try {
            return ipSubnet.contains(otherSource);
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
