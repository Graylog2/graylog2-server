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

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import org.graylog.plugins.netflow.flows.CorruptFlowPacketException;
import org.graylog.plugins.netflow.flows.InvalidFlowVersionException;
import org.graylog.plugins.netflow.utils.ByteBufUtils;

import java.net.InetAddress;

public class NetFlowV5Parser {
    private static final int HEADER_LENGTH = 24;
    private static final int RECORD_LENGTH = 48;

    public static NetFlowV5Packet parsePacket(ByteBuf bb) {
        final int readableBytes = bb.readableBytes();

        final NetFlowV5Header header = parseHeader(bb.slice(bb.readerIndex(), HEADER_LENGTH));
        final int packetLength = HEADER_LENGTH + header.count() * RECORD_LENGTH;
        if (header.count() <= 0 || readableBytes < packetLength) {
            throw new CorruptFlowPacketException("Insufficient data (expected: " + packetLength + " bytes, actual: " + readableBytes + " bytes)");
        }

        final ImmutableList.Builder<NetFlowV5Record> records = ImmutableList.builder();
        int offset = HEADER_LENGTH;
        for (int i = 0; i < header.count(); i++) {
            records.add(parseRecord(bb.slice(offset + bb.readerIndex(), RECORD_LENGTH)));
            offset += RECORD_LENGTH;
        }

        return NetFlowV5Packet.create(header, records.build(), offset);
    }

    /**
     * <pre>
     * | BYTES |     CONTENTS      |                                       DESCRIPTION                                        |
     * |-------|-------------------|------------------------------------------------------------------------------------------|
     * | 0-1   | version           | NetFlow export format version number                                                     |
     * | 2-3   | count             | Number of flows exported in this packet (1-30)                                           |
     * | 4-7   | sys_uptime        | Current time in milliseconds since the export device booted                              |
     * | 8-11  | unix_secs         | Current count of seconds since 0000 UTC 1970                                             |
     * | 12-15 | unix_nsecs        | Residual nanoseconds since 0000 UTC 1970                                                 |
     * | 16-19 | flow_sequence     | Sequence counter of total flows seen                                                     |
     * | 20    | engine_type       | Type of flow-switching engine                                                            |
     * | 21    | engine_id         | Slot number of the flow-switching engine                                                 |
     * | 22-23 | sampling_interval | First two bits hold the sampling mode; remaining 14 bits hold value of sampling interval |
     * </pre>
     */
    private static NetFlowV5Header parseHeader(ByteBuf bb) {
        final int version = bb.readUnsignedShort();
        if (version != 5) {
            throw new InvalidFlowVersionException(version);
        }

        final int count = bb.readUnsignedShort();
        final long sysUptime = bb.readUnsignedInt();
        final long unixSecs = bb.readUnsignedInt();
        final long unixNsecs = bb.readUnsignedInt();
        final long flowSequence = bb.readUnsignedInt();
        final short engineType = bb.readUnsignedByte();
        final short engineId = bb.readUnsignedByte();
        final short sampling = bb.readShort();
        final int samplingMode = (sampling >> 14) & 3;
        final int samplingInterval = sampling & 0x3fff;

        return NetFlowV5Header.create(
                version,
                count,
                sysUptime,
                unixSecs,
                unixNsecs,
                flowSequence,
                engineType,
                engineId,
                samplingMode,
                samplingInterval);
    }

    /**
     * <pre>
     * | BYTES | CONTENTS  |                            DESCRIPTION                             |
     * |-------|-----------|--------------------------------------------------------------------|
     * | 0-3   | srcaddr   | Source IP address                                                  |
     * | 4-7   | dstaddr   | Destination IP address                                             |
     * | 8-11  | nexthop   | IP address of next hop router                                      |
     * | 12-13 | input     | SNMP index of input interface                                      |
     * | 14-15 | output    | SNMP index of output interface                                     |
     * | 16-19 | dPkts     | Packets in the flow                                                |
     * | 20-23 | dOctets   | Total number of Layer 3 bytes in the packets of the flow           |
     * | 24-27 | first     | SysUptime at start of flow                                         |
     * | 28-31 | last      | SysUptime at the time the last packet of the flow was received     |
     * | 32-33 | srcport   | TCP/UDP source port number or equivalent                           |
     * | 34-35 | dstport   | TCP/UDP destination port number or equivalent                      |
     * | 36    | pad1      | Unused (zero) bytes                                                |
     * | 37    | tcp_flags | Cumulative OR of TCP flags                                         |
     * | 38    | prot      | IP protocol type (for example, TCP = 6; UDP = 17)                  |
     * | 39    | tos       | IP type of service (ToS)                                           |
     * | 40-41 | src_as    | Autonomous system number of the source, either origin or peer      |
     * | 42-43 | dst_as    | Autonomous system number of the destination, either origin or peer |
     * | 44    | src_mask  | Source address prefix mask bits                                    |
     * | 45    | dst_mask  | Destination address prefix mask bits                               |
     * | 46-47 | pad2      | Unused (zero) bytes                                                |
     * </pre>
     */
    private static NetFlowV5Record parseRecord(ByteBuf bb) {
        final InetAddress srcAddr = ByteBufUtils.readInetAddress(bb);
        final InetAddress dstAddr = ByteBufUtils.readInetAddress(bb);
        final InetAddress nextHop = ByteBufUtils.readInetAddress(bb);
        final int inputIface = bb.readUnsignedShort();
        final int outputIface = bb.readUnsignedShort();
        final long packetCount = bb.readUnsignedInt();
        final long octetCount = bb.readUnsignedInt();
        final long first = bb.readUnsignedInt();
        final long last = bb.readUnsignedInt();
        final int srcPort = bb.readUnsignedShort();
        final int dstPort = bb.readUnsignedShort();
        bb.readByte(); // unused pad1
        final short tcpFlags = bb.readUnsignedByte();
        final short protocol = bb.readUnsignedByte();
        final short tos = bb.readUnsignedByte();
        final int srcAs = bb.readUnsignedShort();
        final int dstAs = bb.readUnsignedShort();
        final short srcMask = bb.readUnsignedByte();
        final short dstMask = bb.readUnsignedByte();

        return NetFlowV5Record.create(srcAddr, dstAddr, nextHop, inputIface, outputIface, packetCount, octetCount, first, last, srcPort, dstPort, tcpFlags, protocol, tos, srcAs, dstAs, srcMask, dstMask);
    }
}
