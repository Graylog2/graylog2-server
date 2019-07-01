package org.graylog2.shared.system.stats.network;

import org.graylog2.shared.system.stats.OshiService;
import oshi.hardware.NetworkIF;

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
            for (InterfaceAddress that : it.getNetworkInterface().getInterfaceAddresses()) {
                if (localAddress.equalsIgnoreCase(that.getAddress().getHostAddress())) {
                    primaryInterface = it.getName();
                    break;
                }
            }
            NetworkStats.Interface anInterface = NetworkStats.Interface.create(
                it.getName(),
                it.getNetworkInterface().getInterfaceAddresses().stream().map(address -> address.getAddress().getHostAddress()).collect(Collectors.toSet()),
                it.getMacaddr(),
                ((long) it.getMTU()),
                NetworkStats.InterfaceStats.create(
                    it.getPacketsRecv(),
                    it.getInErrors(),
                    0,
                    0,
                    0,
                    it.getPacketsSent(),
                    it.getOutErrors(),
                    0,
                    0,
                    0,
                    0,
                    it.getBytesRecv(),
                    it.getBytesSent()
                )

            );
            ifaces.putIfAbsent(anInterface.name(), anInterface);
        }

        return NetworkStats.create(primaryInterface, ifaces, null);
    }
}
