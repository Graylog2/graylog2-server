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

import org.graylog2.shared.system.stats.SigarService;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Tcp;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;

public class SigarNetworkProbe implements NetworkProbe {
    private final SigarService sigarService;

    @Inject
    public SigarNetworkProbe(SigarService sigarService) {
        this.sigarService = sigarService;
    }

    @Override
    public synchronized NetworkStats networkStats() {
        final Sigar sigar = sigarService.sigar();

        String primaryInterface;
        try {
            final NetInterfaceConfig netInterfaceConfig = sigar.getNetInterfaceConfig(null);
            primaryInterface = netInterfaceConfig.getName();
        } catch (SigarException e) {
            primaryInterface = null;
        }

        final Map<String, NetworkStats.Interface> interfaces = new HashMap<>();
        try {
            final String[] netInterfaceList = firstNonNull(sigar.getNetInterfaceList(), new String[0]);
            for (String interfaceName : netInterfaceList) {
                final NetInterfaceStat netInterfaceStat = sigar.getNetInterfaceStat(interfaceName);
                final NetworkStats.InterfaceStats interfaceStats = NetworkStats.InterfaceStats.create(
                        netInterfaceStat.getRxPackets(),
                        netInterfaceStat.getRxErrors(),
                        netInterfaceStat.getRxDropped(),
                        netInterfaceStat.getRxOverruns(),
                        netInterfaceStat.getRxFrame(),
                        netInterfaceStat.getTxPackets(),
                        netInterfaceStat.getTxErrors(),
                        netInterfaceStat.getTxDropped(),
                        netInterfaceStat.getTxOverruns(),
                        netInterfaceStat.getTxCarrier(),
                        netInterfaceStat.getTxCollisions(),
                        netInterfaceStat.getRxBytes(),
                        netInterfaceStat.getTxBytes());
                final NetInterfaceConfig netInterfaceConfig = sigar.getNetInterfaceConfig(interfaceName);

                final NetworkStats.Interface networkInterface = NetworkStats.Interface.create(
                        netInterfaceConfig.getName(),
                        Collections.singleton(netInterfaceConfig.getAddress()),
                        netInterfaceConfig.getHwaddr(),
                        netInterfaceConfig.getMtu(),
                        interfaceStats);
                interfaces.put(interfaceName, networkInterface);
            }
        } catch (SigarException e) {
            // ignore
        }

        NetworkStats.TcpStats tcpStats;
        try {
            final Tcp tcp = sigar.getTcp();
            tcpStats = NetworkStats.TcpStats.create(
                    tcp.getActiveOpens(),
                    tcp.getPassiveOpens(),
                    tcp.getAttemptFails(),
                    tcp.getEstabResets(),
                    tcp.getCurrEstab(),
                    tcp.getInSegs(),
                    tcp.getOutSegs(),
                    tcp.getRetransSegs(),
                    tcp.getInErrs(),
                    tcp.getOutRsts());
        } catch (SigarException e) {
            tcpStats = null;
        }

        return NetworkStats.create(primaryInterface, interfaces, tcpStats);
    }
}
