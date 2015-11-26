/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
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
package org.graylog2.plugin;

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