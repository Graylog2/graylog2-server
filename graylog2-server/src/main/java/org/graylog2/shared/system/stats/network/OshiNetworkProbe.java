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
package org.graylog2.shared.system.stats.network;

import org.graylog2.shared.system.stats.OshiService;
import oshi.hardware.NetworkIF;
import oshi.software.os.InternetProtocolStats;

import javax.inject.Inject;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.nullToEmpty;

public class OshiNetworkProbe implements NetworkProbe {

    private final OshiService service;

    @Inject
    public OshiNetworkProbe(OshiService service) {
        this.service = service;
    }


    @Override
    public NetworkStats networkStats() {
        final String localAddress;
        String localAddress1;
        try {
            localAddress1 = nullToEmpty(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            localAddress1 = "0.0.0.0";
        }
        localAddress = localAddress1;
        String primaryInterface = "";


        Map<String, NetworkStats.Interface> ifaces = new HashMap<>();
        for (NetworkIF it : service.getHal().getNetworkIFs()) {
            for (InterfaceAddress that : it.queryNetworkInterface().getInterfaceAddresses()) {
                if (localAddress.equalsIgnoreCase(that.getAddress().getHostAddress())) {
                    primaryInterface = it.getName();
                    break;
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
