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
package org.graylog2.shared.system.stats.network;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.nullToEmpty;

public class JmxNetworkProbe implements NetworkProbe {
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    @Override
    public NetworkStats networkStats() {
        String primaryInterface = null;
        final Map<String, NetworkStats.Interface> interfaces = new HashMap<>();
        try {
            final String localAddress = nullToEmpty(InetAddress.getLocalHost().getHostAddress());

            final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netIf : Collections.list(networkInterfaces)) {
                final String name = netIf.getName();
                final NetworkStats.Interface networkInterface = buildInterface(netIf);

                interfaces.put(name, networkInterface);

                if (networkInterface.addresses().contains(localAddress)) {
                    primaryInterface = name;
                }
            }
        } catch (Exception e) {
            // ignore
        }

        return NetworkStats.create(primaryInterface, interfaces, null);
    }

    private NetworkStats.Interface buildInterface(NetworkInterface networkInterface) throws SocketException {
        final String macAddress = bytesToMacAddressString(networkInterface.getHardwareAddress());

        List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
        final Set<String> addresses = new HashSet<>(interfaceAddresses.size());
        for (InterfaceAddress interfaceAddress : interfaceAddresses) {
            addresses.add(interfaceAddress.getAddress().getHostAddress());
        }

        return NetworkStats.Interface.create(
                networkInterface.getName(),
                addresses,
                macAddress,
                networkInterface.getMTU(),
                null
        );
    }

    private String bytesToMacAddressString(byte[] bytes) {
        if (null == bytes) {
            return "00:00:00:00:00:00";
        }

        char[] hexChars = new char[bytes.length * 2 + 5];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 3] = hexArray[v >>> 4];
            hexChars[i * 3 + 1] = hexArray[v & 0x0F];

            // Skip the last colon
            if (i < bytes.length - 1) {
                hexChars[i * 3 + 2] = ':';
            }
        }
        return new String(hexChars);
    }
}
