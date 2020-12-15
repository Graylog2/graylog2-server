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
package org.graylog.plugins.netflow.v5;

import com.google.auto.value.AutoValue;

import java.net.InetAddress;

@AutoValue
public abstract class NetFlowV5Record {
    // bytes 0-3
    public abstract InetAddress srcAddr();

    // bytes 4-7
    public abstract InetAddress dstAddr();

    // bytes 8-11
    public abstract InetAddress nextHop();

    // bytes 12-13, snmp index of input interface
    public abstract int inputIface();

    // bytes 14-15, snmp index of output interface
    public abstract int outputIface();

    // bytes 16-19, packets in flow
    public abstract long packetCount();

    // bytes 20-23, total number of L3 bytes in the packets of the flow
    public abstract long octetCount();

    // bytes 24-27, sysuptime at start of flow
    public abstract long first();

    // bytes 28-31, sysuptime at the time the last packet of the flow was
    // received
    public abstract long last();

    // bytes 32-33
    public abstract int srcPort();

    // bytes 34-35
    public abstract int dstPort();

    // bytes 37
    public abstract short tcpFlags();

    // bytes 38, ip protocol type (e.g. tcp = 6, udp = 17)
    public abstract short protocol();

    // bytes 39, type of service
    public abstract short tos();

    // bytes 40-41, source AS number
    public abstract int srcAs();

    // bytes 42-43, destination AS number
    public abstract int dstAs();

    // bytes 44
    public abstract short srcMask();

    // bytes 45
    public abstract short dstMask();

    static NetFlowV5Record create(InetAddress srcAddr,
                                  InetAddress dstAddr,
                                  InetAddress nextHop,
                                  int inputIface,
                                  int outputIface,
                                  long packetCount,
                                  long octetCount,
                                  long first,
                                  long last,
                                  int srcPort,
                                  int dstPort,
                                  short tcpFlags,
                                  short protocol,
                                  short tos,
                                  int srcAs,
                                  int dstAs,
                                  short srcMask,
                                  short dstMask) {
        return new AutoValue_NetFlowV5Record(srcAddr, dstAddr, nextHop, inputIface, outputIface, packetCount, octetCount, first, last, srcPort, dstPort, tcpFlags, protocol, tos, srcAs, dstAs, srcMask, dstMask
        );
    }
}
