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
package org.graylog2.plugin;

import org.graylog2.shared.SuppressForbidden;
import org.junit.Test;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

public class ResolvableInetSocketAddressTest {

    @Test
    public void testWrapWithNull() throws Exception {
        assertThat(ResolvableInetSocketAddress.wrap(null)).isNull();
    }

    @Test
    public void testWrap() throws Exception {
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getLoopbackAddress(), 12345);
        final ResolvableInetSocketAddress address = ResolvableInetSocketAddress.wrap(inetSocketAddress);

        assertThat(address.getInetSocketAddress()).isSameAs(inetSocketAddress);
    }

    @Test
    @SuppressForbidden("Intentional invocation of InetSocketAddress#getHostName()")
    public void testReverseLookup() throws Exception {
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getLoopbackAddress(), 12345);
        final ResolvableInetSocketAddress address = new ResolvableInetSocketAddress(inetSocketAddress);

        assertThat(address.isReverseLookedUp()).isFalse();
        assertThat(address.reverseLookup()).isEqualTo(inetSocketAddress.getHostName());
        assertThat(address.isReverseLookedUp()).isTrue();
    }

    @Test
    public void testIsReverseLookedUp() throws Exception {
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getLoopbackAddress(), 12345);
        final ResolvableInetSocketAddress address = new ResolvableInetSocketAddress(inetSocketAddress);

        assertThat(address.isReverseLookedUp()).isFalse();

        address.reverseLookup();

        assertThat(address.isReverseLookedUp()).isTrue();
    }

    @Test
    public void testIsUnresolved() throws Exception {
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getLoopbackAddress(), 12345);
        final ResolvableInetSocketAddress address = new ResolvableInetSocketAddress(inetSocketAddress);

        assertThat(address.isUnresolved()).isEqualTo(inetSocketAddress.isUnresolved());
    }

    @Test
    public void testGetAddress() throws Exception {
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getLoopbackAddress(), 12345);
        final ResolvableInetSocketAddress address = new ResolvableInetSocketAddress(inetSocketAddress);

        assertThat(address.getAddress()).isEqualTo(inetSocketAddress.getAddress());
    }

    @Test
    public void testGetAddressBytes() throws Exception {
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getLoopbackAddress(), 12345);
        final ResolvableInetSocketAddress address = new ResolvableInetSocketAddress(inetSocketAddress);

        assertThat(address.getAddressBytes()).isEqualTo(inetSocketAddress.getAddress().getAddress());
    }

    @Test
    public void testGetPort() throws Exception {
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getLoopbackAddress(), 12345);
        final ResolvableInetSocketAddress address = new ResolvableInetSocketAddress(inetSocketAddress);

        assertThat(address.getPort()).isEqualTo(inetSocketAddress.getPort());
    }

    @Test
    @SuppressForbidden("Intentional invocation of InetSocketAddress#getHostName()")
    public void testGetHostName() throws Exception {
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getLoopbackAddress(), 12345);
        final ResolvableInetSocketAddress address = new ResolvableInetSocketAddress(inetSocketAddress);

        assertThat(address.getHostName()).isNull();

        address.reverseLookup();

        assertThat(address.getHostName()).isEqualTo(inetSocketAddress.getHostName());
    }

    @Test
    public void testGetInetSocketAddress() throws Exception {
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getLoopbackAddress(), 12345);
        final ResolvableInetSocketAddress address = new ResolvableInetSocketAddress(inetSocketAddress);

        assertThat(address.getInetSocketAddress()).isEqualTo(inetSocketAddress);
    }

    @Test
    public void testToString() throws Exception {
        final InetAddress localHost = Inet4Address.getLoopbackAddress();
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(localHost, 12345);
        final ResolvableInetSocketAddress address = new ResolvableInetSocketAddress(inetSocketAddress);

        assertThat(address.toString()).isEqualTo(address.getAddress().getHostAddress() + ":" + address.getPort());

        address.reverseLookup();

        assertThat(address.toString()).isEqualTo(address.getHostName() + ":" + address.getPort());
    }
}