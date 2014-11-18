/*
 * Copyright 2014 TORCH GmbH
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
