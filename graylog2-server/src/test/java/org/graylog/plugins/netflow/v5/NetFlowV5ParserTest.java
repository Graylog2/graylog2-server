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

import com.google.common.io.Resources;
import com.google.common.net.InetAddresses;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.pkts.Pcap;
import io.pkts.packet.UDPPacket;
import io.pkts.protocol.Protocol;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NetFlowV5ParserTest {
    @Test
    public void testParse1() throws IOException {
        final byte[] b = Resources.toByteArray(Resources.getResource("netflow-data/netflow-v5-1.dat"));
        NetFlowV5Packet packet = NetFlowV5Parser.parsePacket(Unpooled.wrappedBuffer(b));
        assertNotNull(packet);

        NetFlowV5Header h = packet.header();
        assertEquals(5, h.version());
        assertEquals(2, h.count());
        assertEquals(3381L, h.sysUptime());
        assertEquals(1430591888L, h.unixSecs());
        assertEquals(280328000, h.unixNsecs());

        final List<NetFlowV5Record> records = packet.records();
        assertEquals(2, records.size());

        final NetFlowV5Record record1 = records.get(0);
        assertEquals(InetAddresses.forString("10.0.2.15"), record1.dstAddr());
        assertEquals(6, record1.protocol());
        assertEquals(0, record1.srcAs());
        assertEquals(InetAddresses.forString("10.0.2.2"), record1.srcAddr());
        assertEquals(2577L, record1.last());
        assertEquals(22, record1.dstPort());
        assertEquals(230L, record1.octetCount());
        assertEquals(54435, record1.srcPort());
        assertEquals(0, record1.srcMask());
        assertEquals(0, record1.tos());
        assertEquals(0, record1.inputIface());
        assertEquals(InetAddresses.forString("0.0.0.0"), record1.nextHop());
        assertEquals(16, record1.tcpFlags());
        assertEquals(0, record1.dstAs());
        assertEquals(0, record1.outputIface());
        assertEquals(4294967295L, record1.first());
        assertEquals(0, record1.dstMask());
        assertEquals(5L, record1.packetCount());


        final NetFlowV5Record record2 = records.get(1);
        assertEquals(InetAddresses.forString("10.0.2.2"), record2.dstAddr());
        assertEquals(6, record2.protocol());
        assertEquals(0, record2.srcAs());
        assertEquals(InetAddresses.forString("10.0.2.15"), record2.srcAddr());
        assertEquals(2577L, record2.last());
        assertEquals(54435, record2.dstPort());
        assertEquals(304L, record2.octetCount());
        assertEquals(22, record2.srcPort());
        assertEquals(0, record2.srcMask());
        assertEquals(0, record2.tos());
        assertEquals(0, record2.inputIface());
        assertEquals(InetAddresses.forString("0.0.0.0"), record2.nextHop());
        assertEquals(24, record2.tcpFlags());
        assertEquals(0, record2.dstAs());
        assertEquals(0, record2.outputIface());
        assertEquals(4294967295L, record2.first());
        assertEquals(0, record2.dstMask());
        assertEquals(4L, record2.packetCount());
    }

    @Test
    public void testParse2() throws IOException {
        final byte[] b = Resources.toByteArray(Resources.getResource("netflow-data/netflow-v5-2.dat"));
        NetFlowV5Packet packet = NetFlowV5Parser.parsePacket(Unpooled.wrappedBuffer(b));
        assertNotNull(packet);

        NetFlowV5Header h = packet.header();
        assertEquals(5, h.version());
        assertEquals(30, h.count());
        assertEquals(234994, h.sysUptime());
        assertEquals(1369017138, h.unixSecs());
        assertEquals(805, h.unixNsecs());

        assertEquals(30, packet.records().size());
        final NetFlowV5Record r = packet.records().get(0);
        assertEquals(InetAddresses.forString("192.168.124.20"), r.dstAddr());
        assertEquals(6, r.protocol());
        assertEquals(0, r.srcAs());
        assertEquals(InetAddresses.forString("14.63.211.15"), r.srcAddr());
        assertEquals(202992L, r.last());
        assertEquals(47994, r.dstPort());
        assertEquals(317221L, r.octetCount());
        assertEquals(80, r.srcPort());
        assertEquals(0, r.srcMask());
        assertEquals(0, r.tos());
        assertEquals(0, r.inputIface());
        assertEquals(InetAddresses.forString("0.0.0.0"), r.nextHop());
        assertEquals(27, r.tcpFlags());
        assertEquals(0, r.dstAs());
        assertEquals(0, r.outputIface());
        assertEquals(202473L, r.first());
        assertEquals(0, r.dstMask());
        assertEquals(110L, r.packetCount());
    }

    @Test
    public void pcap_softflowd_NetFlowV5() throws Exception {
        final List<NetFlowV5Record> allRecords = new ArrayList<>();
        try (InputStream inputStream = Resources.getResource("netflow-data/netflow5.pcap").openStream()) {
            final Pcap pcap = Pcap.openStream(inputStream);
            pcap.loop(packet -> {
                        if (packet.hasProtocol(Protocol.UDP)) {
                            final UDPPacket udp = (UDPPacket) packet.getPacket(Protocol.UDP);
                            final ByteBuf byteBuf = Unpooled.wrappedBuffer(udp.getPayload().getArray());
                            final NetFlowV5Packet netFlowV5Packet = NetFlowV5Parser.parsePacket(byteBuf);
                            assertThat(netFlowV5Packet).isNotNull();
                            allRecords.addAll(netFlowV5Packet.records());
                        }
                        return true;
                    }
            );
        }
        assertThat(allRecords).hasSize(4);
    }

    @Test
    public void pcap_pmacctd_NetFlowV5() throws Exception {
        final List<NetFlowV5Record> allRecords = new ArrayList<>();
        try (InputStream inputStream = Resources.getResource("netflow-data/pmacctd-netflow5.pcap").openStream()) {
            final Pcap pcap = Pcap.openStream(inputStream);
            pcap.loop(packet -> {
                        if (packet.hasProtocol(Protocol.UDP)) {
                            final UDPPacket udp = (UDPPacket) packet.getPacket(Protocol.UDP);
                            final ByteBuf byteBuf = Unpooled.wrappedBuffer(udp.getPayload().getArray());
                            final NetFlowV5Packet netFlowV5Packet = NetFlowV5Parser.parsePacket(byteBuf);
                            assertThat(netFlowV5Packet).isNotNull();
                            allRecords.addAll(netFlowV5Packet.records());
                        }
                        return true;
                    }
            );
        }
        assertThat(allRecords).hasSize(42);
    }

    @Test
    public void pcap_netgraph_NetFlowV5() throws Exception {
        final List<NetFlowV5Record> allRecords = new ArrayList<>();
        try (InputStream inputStream = Resources.getResource("netflow-data/netgraph-netflow5.pcap").openStream()) {
            final Pcap pcap = Pcap.openStream(inputStream);
            pcap.loop(packet -> {
                        if (packet.hasProtocol(Protocol.UDP)) {
                            final UDPPacket udp = (UDPPacket) packet.getPacket(Protocol.UDP);
                            final ByteBuf byteBuf = Unpooled.wrappedBuffer(udp.getPayload().getArray());
                            final NetFlowV5Packet netFlowV5Packet = NetFlowV5Parser.parsePacket(byteBuf);
                            assertThat(netFlowV5Packet).isNotNull();
                            allRecords.addAll(netFlowV5Packet.records());
                        }
                        return true;
                    }
            );
        }
        assertThat(allRecords).hasSize(120);
    }
}
