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
package org.graylog.plugins.netflow.flows;

import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.netflow.utils.ByteBufUtils;
import org.graylog.plugins.netflow.utils.Protocol;
import org.graylog.plugins.netflow.v5.NetFlowV5Header;
import org.graylog.plugins.netflow.v5.NetFlowV5Record;
import org.graylog.plugins.netflow.v9.NetFlowV9BaseRecord;
import org.graylog.plugins.netflow.v9.NetFlowV9Header;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Map;

public class NetFlowFormatter {
    private static final String MF_VERSION = "nf_version";
    private static final String MF_FLOW_PACKET_ID = "nf_flow_packet_id";
    private static final String MF_TOS = "nf_tos";
    private static final String MF_SRC = "nf_src";
    private static final String MF_SRC_ADDRESS = "nf_src_address";
    private static final String MF_SRC_TOS = "nf_src_tos";
    private static final String MF_DST = "nf_dst";
    private static final String MF_DST_ADDRESS = "nf_dst_address";
    private static final String MF_DST_TOS = "nf_dst_tos";
    private static final String MF_NEXT_HOP = "nf_next_hop";
    private static final String MF_SRC_PORT = "nf_src_port";
    private static final String MF_DST_PORT = "nf_dst_port";
    private static final String MF_SRC_MASK = "nf_src_mask";
    private static final String MF_DST_MASK = "nf_dst_mask";
    private static final String MF_SRC_AS = "nf_src_as";
    private static final String MF_DST_AS = "nf_dst_as";
    private static final String MF_PROTO = "nf_proto";
    private static final String MF_PROTO_NAME = "nf_proto_name";
    private static final String MF_TCP_FLAGS = "nf_tcp_flags";
    private static final String MF_START = "nf_start";
    private static final String MF_STOP = "nf_stop";
    private static final String MF_BYTES = "nf_bytes";
    private static final String MF_PKTS = "nf_pkts";
    private static final String MF_SNMP_INPUT = "nf_snmp_input";
    private static final String MF_SNMP_OUTPUT = "nf_snmp_output";

    private static String toMessageString(NetFlowV5Record record) {
        return String.format(Locale.ROOT, "NetFlowV5 [%s]:%d <> [%s]:%d proto:%d pkts:%d bytes:%d",
                record.srcAddr().getHostAddress(), record.srcPort(),
                record.dstAddr().getHostAddress(), record.dstPort(),
                record.protocol(), record.packetCount(), record.octetCount());
    }

    private static String toMessageString(NetFlowV9BaseRecord record) {
        final ImmutableMap<String, Object> fields = record.fields();
        final long packetCount = (long) fields.getOrDefault("in_pkts", 0L);
        long octetCount = (long) fields.getOrDefault("in_bytes", 0L);
        if (octetCount == 0L) {
            octetCount = (long) fields.getOrDefault("fwd_flow_delta_bytes", 0L);
        }
        final String srcAddr = (String) fields.get("ipv4_src_addr");
        final String dstAddr = (String) fields.get("ipv4_dst_addr");
        final Integer srcPort = (Integer) fields.get("l4_src_port");
        final Integer dstPort = (Integer) fields.get("l4_dst_port");
        final Short protocol = (Short) fields.get("protocol");

        return String.format(Locale.ROOT, "NetFlowV9 [%s]:%d <> [%s]:%d proto:%d pkts:%d bytes:%d",
                srcAddr, srcPort,
                dstAddr, dstPort,
                protocol, packetCount, octetCount);
    }

    public static Message toMessage(NetFlowV5Header header,
                                    NetFlowV5Record record,
                                    @Nullable InetSocketAddress sender) {
        final String source = sender == null ? null : sender.getAddress().getHostAddress();
        final long timestamp = header.unixSecs() * 1000L + (header.unixNsecs() / 1000000L);
        final Message message = new Message(toMessageString(record), source, new DateTime(timestamp, DateTimeZone.UTC));

        message.addField(MF_VERSION, 5);
        message.addField(MF_FLOW_PACKET_ID, header.flowSequence());
        message.addField(MF_TOS, record.tos());
        message.addField(MF_SRC, record.srcAddr().getHostAddress() + ":" + record.srcPort());
        message.addField(MF_SRC_ADDRESS, record.srcAddr().getHostAddress());
        message.addField(MF_DST, record.dstAddr().getHostAddress() + ":" + record.dstPort());
        message.addField(MF_DST_ADDRESS, record.dstAddr().getHostAddress());
        if (!ByteBufUtils.DEFAULT_INET_ADDRESS.equals(record.nextHop())) {
            message.addField(MF_NEXT_HOP, record.nextHop().getHostAddress());
        }
        message.addField(MF_SRC_PORT, record.srcPort());
        message.addField(MF_DST_PORT, record.dstPort());
        message.addField(MF_SRC_MASK, record.srcMask());
        message.addField(MF_DST_MASK, record.dstMask());
        message.addField(MF_SRC_AS, record.srcAs());
        message.addField(MF_DST_AS, record.dstAs());

        message.addField(MF_PROTO, record.protocol());
        final Protocol protocol = Protocol.getByNumber(record.protocol());
        if (protocol != null) {
            message.addField(MF_PROTO_NAME, protocol.getAlias());
        }
        message.addField(MF_TCP_FLAGS, record.tcpFlags());
        if (record.first() > 0) {
            long start = timestamp - (header.sysUptime() - record.first());
            message.addField(MF_START, new DateTime(start, DateTimeZone.UTC));
        }
        if (record.last() > 0) {
            long stop = timestamp - (header.sysUptime() - record.last());
            message.addField(MF_STOP, new DateTime(stop, DateTimeZone.UTC));
        }
        message.addField(MF_BYTES, record.octetCount());
        message.addField(MF_PKTS, record.packetCount());
        message.addField(MF_SNMP_INPUT, record.inputIface());
        message.addField(MF_SNMP_OUTPUT, record.outputIface());

        return message;
    }

    public static Message toMessage(NetFlowV9Header header,
                                    NetFlowV9BaseRecord record,
                                    @Nullable InetSocketAddress sender) {
        final String source = sender == null ? null : sender.getAddress().getHostAddress();
        final long timestamp = header.unixSecs() * 1000L;
        final Message message = new Message(toMessageString(record), source, new DateTime(timestamp, DateTimeZone.UTC));

        final Map<String, Object> fields = record.fields();

        message.addField(MF_VERSION, 9);
        fields.forEach((key, value) -> message.addField("nf_" + key, value));

        final String srcAddr = (String) fields.get("ipv4_src_addr");
        final String dstAddr = (String) fields.get("ipv4_dst_addr");
        final Object srcPort = fields.get("l4_src_port");
        final Object dstPort = fields.get("l4_dst_port");
        final String ipv4NextHop = (String) fields.get("ipv4_next_hop");
        final Long first = (Long) fields.get("first_switched");
        final Long last = (Long) fields.get("last_switched");

        message.addField(MF_FLOW_PACKET_ID, header.sequence());
        message.addField(MF_TOS, fields.get("ip_tos"));
        message.addField(MF_SRC_TOS, fields.get("ip_src_tos"));
        message.addField(MF_DST_TOS, fields.get("ip_dst_tos"));
        message.addField(MF_SRC, srcAddr + ":" + srcPort);
        message.addField(MF_SRC_ADDRESS, srcAddr);
        message.addField(MF_DST, dstAddr + ":" + dstPort);
        message.addField(MF_DST_ADDRESS, dstAddr);
        if (!ByteBufUtils.DEFAULT_INET_ADDRESS.getHostAddress().equals(ipv4NextHop)) {
            message.addField(MF_NEXT_HOP, ipv4NextHop);
        }
        message.addField(MF_SRC_PORT, srcPort);
        message.addField(MF_DST_PORT, dstPort);
        message.addField(MF_SRC_MASK, fields.get("src_mask"));
        message.addField(MF_DST_MASK, fields.get("dst_mask"));
        message.addField(MF_SRC_AS, fields.get("src_as"));
        message.addField(MF_DST_AS, fields.get("dst_as"));
        final Object protocol = fields.get("protocol");
        if (protocol != null) {
            message.addField(MF_PROTO, protocol);
            short protocolNumber = ((Number) protocol).shortValue();
            final Protocol protocolInfo = Protocol.getByNumber(protocolNumber);
            if (protocolInfo != null) {
                message.addField(MF_PROTO_NAME, protocolInfo.getAlias());
            }
        }
        message.addField(MF_TCP_FLAGS, fields.get("tcp_flags"));

        if (first != null && first > 0) {
            long start = timestamp - (header.sysUptime() - first);
            message.addField(MF_START, new DateTime(start, DateTimeZone.UTC));
        }
        if (last != null && last > 0) {
            long stop = timestamp - (header.sysUptime() - last);
            message.addField(MF_STOP, new DateTime(stop, DateTimeZone.UTC));
        }
        message.addField(MF_BYTES, fields.get("in_bytes"));
        message.addField(MF_PKTS, fields.get("in_pkts"));
        message.addField(MF_SNMP_INPUT, fields.get("input_snmp"));
        message.addField(MF_SNMP_OUTPUT, fields.get("output_snmp"));

        return message;
    }
}
