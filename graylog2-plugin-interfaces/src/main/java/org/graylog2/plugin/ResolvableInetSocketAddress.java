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
package org.graylog2.plugin;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * InetSocketAddress does not support finding out whether an IP address has been reverse looked up or not.<br/>
 * However, we need to avoid triggering a name service lookup unless specifically asked to.<br/>
 * This class exists to make the reverse lookup step explicit in the code.
 */
public class ResolvableInetSocketAddress {
    private final InetSocketAddress inetSocketAddress;
    private boolean reverseLookedUp = false;

    public ResolvableInetSocketAddress(InetSocketAddress inetSocketAddress) {
        this.inetSocketAddress = inetSocketAddress;
    }

    public static ResolvableInetSocketAddress wrap(InetSocketAddress socketAddress) {
        if (socketAddress == null) return null;
        return new ResolvableInetSocketAddress(socketAddress);
    }

    public String reverseLookup() {
        final String hostName = inetSocketAddress.getHostName();
        reverseLookedUp = true;
        return hostName;
    }

    public boolean isReverseLookedUp() {
        return reverseLookedUp;
    }

    public boolean isUnresolved() {
        return inetSocketAddress.isUnresolved();
    }

    public InetAddress getAddress() {
        return inetSocketAddress.getAddress();
    }

    public byte[] getAddressBytes() {
        return inetSocketAddress.getAddress().getAddress();
    }

    public int getPort() {
        return inetSocketAddress.getPort();
    }

    public String getHostName() {
        if (isReverseLookedUp()) {
            return inetSocketAddress.getHostName();
        }
        return null;
    }

    public InetSocketAddress getInetSocketAddress() {
        return inetSocketAddress;
    }
}
