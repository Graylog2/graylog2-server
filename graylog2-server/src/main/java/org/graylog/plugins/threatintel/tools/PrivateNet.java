/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.threatintel.tools;

import com.google.common.net.InetAddresses;
import org.graylog2.utilities.IpSubnet;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

@SuppressWarnings("UnstableApiUsage")
public class PrivateNet {

    private static IpSubnet UNIQUE_LOCAL_ADDR_MASK = null;
    static {
        try {
            // RFC 4193: https://tools.ietf.org/html/rfc4193#section-3.1
            UNIQUE_LOCAL_ADDR_MASK = new IpSubnet("FC00::/7");
        } catch (UnknownHostException ignored) {
        }

    }
   /**
     * Checks if an IP address is part of a private network as defined in RFC 1918 (for IPv4) and RFC 4193 (for IPv6).
    *
     *
     * @param ip The IP address to check
     * @return true if IP address is in a private subnet, false if not or unknown
     */
    public static boolean isInPrivateAddressSpace(String ip) {
        InetAddress inetAddress = InetAddresses.forString(ip);
        if (inetAddress instanceof Inet6Address) {
            // Inet6Address#isSiteLocalAddress is wrong: it only checks for FEC0:: prefixes, which is deprecated in RFC 3879
            // instead we need to check for unique local addresses, which are in FC00::/7 (in practice assigned are in FD00::/8,
            // but the RFC allows others in the future)
            return UNIQUE_LOCAL_ADDR_MASK.contains(inetAddress);
        }
        return inetAddress.isSiteLocalAddress();
    }

}

