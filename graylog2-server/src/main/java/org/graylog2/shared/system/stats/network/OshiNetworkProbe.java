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

import org.apache.logging.log4j.util.Strings;
import org.graylog2.shared.system.stats.OshiService;
import org.graylog2.utilities.IpSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.hardware.NetworkIF;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;

import javax.inject.Inject;
import java.net.InterfaceAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class OshiNetworkProbe implements NetworkProbe {
    private static final Logger LOG = LoggerFactory.getLogger(OshiNetworkProbe.class);

    private final OshiService service;

    @Inject
    public OshiNetworkProbe(OshiService service) {
        this.service = service;
    }


    @Override
    public NetworkStats networkStats() {
        String primaryInterface = "";

        final NetworkParams networkParams = service.getOs().getNetworkParams();
        final String defaultGateway = networkParams.getIpv4DefaultGateway();

        Map<String, NetworkStats.Interface> ifaces = new HashMap<>();
        for (NetworkIF it : service.getHal().getNetworkIFs()) {
            if (Strings.isNotBlank(defaultGateway)) {
                for (InterfaceAddress that : it.queryNetworkInterface().getInterfaceAddresses()) {
                    try {
                        final IpSubnet ipSubnet = new IpSubnet(that.getAddress().getHostAddress() + "/" + that.getNetworkPrefixLength());
                        if (ipSubnet.contains(defaultGateway)) {
                            primaryInterface = it.getName();
                        }
                    } catch (UnknownHostException e) {
                        LOG.warn("Couldn't find primary interface", e);
                    }
                }
            }

            NetworkStats.Interface anInterface = NetworkStats.Interface.create(
                    it.getName(),
                    it.queryNetworkInterface().getInterfaceAddresses().stream().map(address -> address.getAddress().getHostAddress()).collect(Collectors.toSet()),
                    it.getMacaddr(),
                    it.getMTU(),
                    NetworkStats.InterfaceStats.create(
                            it.getPacketsRecv(),
                            it.getInErrors(),
                            it.getInDrops(),
                            -1,
                            -1,
                            it.getPacketsSent(),
                            it.getOutErrors(),
                            -1,
                            -1,
                            -1,
                            it.getCollisions(),
                            it.getBytesRecv(),
                            it.getBytesSent()
                    )

            );
            ifaces.putIfAbsent(anInterface.name(), anInterface);
        }

        final InternetProtocolStats stats = service.getOs().getInternetProtocolStats();
        final InternetProtocolStats.TcpStats ipv4Stats = stats.getTCPv4Stats();
        final InternetProtocolStats.TcpStats ipv6Stats = stats.getTCPv6Stats();

        final NetworkStats.TcpStats tcpStats = NetworkStats.TcpStats.create(
                ipv4Stats.getConnectionsActive() + ipv6Stats.getConnectionsActive(),
                ipv4Stats.getConnectionsPassive() + ipv6Stats.getConnectionsPassive(),
                ipv4Stats.getConnectionFailures() + ipv6Stats.getConnectionFailures(),
                ipv4Stats.getConnectionsReset() + ipv6Stats.getConnectionsReset(),
                ipv4Stats.getConnectionsEstablished() + ipv6Stats.getConnectionsEstablished(),
                ipv4Stats.getSegmentsReceived() + ipv6Stats.getSegmentsReceived(),
                ipv4Stats.getSegmentsSent() + ipv6Stats.getSegmentsSent(),
                ipv4Stats.getSegmentsRetransmitted() + ipv6Stats.getSegmentsRetransmitted(),
                ipv4Stats.getInErrors() + ipv6Stats.getInErrors(),
                ipv4Stats.getOutResets() + ipv6Stats.getOutResets()
        );

        return NetworkStats.create(primaryInterface, ifaces, tcpStats);
    }
}
