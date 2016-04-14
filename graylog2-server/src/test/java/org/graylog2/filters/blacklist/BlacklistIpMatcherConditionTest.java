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

import com.google.common.net.InetAddresses;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BlacklistIpMatcherConditionTest {
    @Test
    public void matchesInetAddressIPv4() throws Exception {
        final BlacklistIpMatcherCondition condition = new BlacklistIpMatcherCondition();
        condition.setPattern("192.0.2.0/24");

        assertThat(condition.matchesInetAddress(InetAddresses.forString("192.0.2.0"))).isTrue();
        assertThat(condition.matchesInetAddress(InetAddresses.forString("192.0.2.42"))).isTrue();
        assertThat(condition.matchesInetAddress(InetAddresses.forString("192.0.2.255"))).isTrue();
        assertThat(condition.matchesInetAddress(InetAddresses.forString("192.0.3.0"))).isFalse();
        assertThat(condition.matchesInetAddress(InetAddresses.forString("192.0.4.0"))).isFalse();
        assertThat(condition.matchesInetAddress(InetAddresses.forString("::1"))).isFalse();
        assertThat(condition.matchesInetAddress(InetAddresses.forString("127.0.0.1"))).isFalse();
    }

    @Test
    public void matchesInetAddressIPv6() throws Exception {
        final BlacklistIpMatcherCondition condition = new BlacklistIpMatcherCondition();
        condition.setPattern("2001:DB8::/32");

        assertThat(condition.matchesInetAddress(InetAddresses.forString("2001:DB8::0"))).isTrue();
        assertThat(condition.matchesInetAddress(InetAddresses.forString("2001:DB8::42"))).isTrue();
        assertThat(condition.matchesInetAddress(InetAddresses.forString("2001:0db8:ffff:ffff:ffff:ffff:ffff:ffff"))).isTrue();
        assertThat(condition.matchesInetAddress(InetAddresses.forString("2001:DB9::0"))).isFalse();
        assertThat(condition.matchesInetAddress(InetAddresses.forString("2001:0db7:ffff:ffff:ffff:ffff:ffff:ffff"))).isFalse();
        assertThat(condition.matchesInetAddress(InetAddresses.forString("::1"))).isFalse();
        assertThat(condition.matchesInetAddress(InetAddresses.forString("127.0.0.1"))).isFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void matchesInetAddressWithInvalidPattern() throws Exception {
        final BlacklistIpMatcherCondition condition = new BlacklistIpMatcherCondition();
        condition.setPattern("foobar");
    }
}
