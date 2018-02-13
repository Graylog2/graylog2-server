/*
 * Copyright 2013 Eediom Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
