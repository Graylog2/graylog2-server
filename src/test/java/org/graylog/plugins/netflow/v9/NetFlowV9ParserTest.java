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
package org.graylog.plugins.netflow.v9;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.pkts.Pcap;
import io.pkts.packet.UDPPacket;
import io.pkts.protocol.Protocol;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NetFlowV9ParserTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private NetFlowV9TemplateCache cache;
    private NetFlowV9FieldTypeRegistry typeRegistry;

    @Before
    public void setUp() throws IOException {
        cache = new NetFlowV9TemplateCache(
                1000L,
                temporaryFolder.newFile().toPath(),
                30 * 60,
                Executors.newSingleThreadScheduledExecutor(),
                objectMapper);
        typeRegistry = NetFlowV9FieldTypeRegistry.create();

    }

    @Test
    public void testParse() throws IOException {
        final byte[] b1 = Resources.toByteArray(Resources.getResource("netflow-data/netflow-v9-2-1.dat"));
        final byte[] b2 = Resources.toByteArray(Resources.getResource("netflow-data/netflow-v9-2-2.dat"));
        final byte[] b3 = Resources.toByteArray(Resources.getResource("netflow-data/netflow-v9-2-3.dat"));

        // check header
        NetFlowV9Packet p1 = NetFlowV9Parser.parsePacket(Unpooled.wrappedBuffer(b1), cache, typeRegistry);
        assertEquals(9, p1.header().version());
        assertEquals(3, p1.header().count());
        assertEquals(0, p1.header().sequence());
        assertEquals(42212, p1.header().sysUptime());
        assertEquals(1369122709, p1.header().unixSecs());
        assertEquals(106, p1.header().sourceId());

        // check templates
        assertEquals(2, p1.templates().size());
        assertNotNull(p1.optionTemplate());

        NetFlowV9Template t1 = p1.templates().get(0);
        assertEquals(257, t1.templateId());
        assertEquals(18, t1.fieldCount());

        List<NetFlowV9FieldDef> d1 = t1.definitions();
        assertEquals("in_bytes", name(d1.get(0)));
        assertEquals("in_pkts", name(d1.get(1)));
        assertEquals("protocol", name(d1.get(2)));
        assertEquals("src_tos", name(d1.get(3)));
        assertEquals("tcp_flags", name(d1.get(4)));
        assertEquals("l4_src_port", name(d1.get(5)));
        assertEquals("ipv4_src_addr", name(d1.get(6)));
        assertEquals("src_mask", name(d1.get(7)));
        assertEquals("input_snmp", name(d1.get(8)));
        assertEquals("l4_dst_port", name(d1.get(9)));
        assertEquals("ipv4_dst_addr", name(d1.get(10)));
        assertEquals("dst_mask", name(d1.get(11)));
        assertEquals("output_snmp", name(d1.get(12)));
        assertEquals("ipv4_next_hop", name(d1.get(13)));
        assertEquals("src_as", name(d1.get(14)));
        assertEquals("dst_as", name(d1.get(15)));
        assertEquals("last_switched", name(d1.get(16)));
        assertEquals("first_switched", name(d1.get(17)));

        NetFlowV9Template t2 = p1.templates().get(1);
        assertEquals(258, t2.templateId());
        assertEquals(18, t2.fieldCount());

        NetFlowV9Packet p2 = NetFlowV9Parser.parsePacket(Unpooled.wrappedBuffer(b2), cache, typeRegistry);
        NetFlowV9BaseRecord r2 = p2.records().get(0);
        Map<String, Object> f2 = r2.fields();
        assertEquals(2818L, f2.get("in_bytes"));
        assertEquals(8L, f2.get("in_pkts"));
        assertEquals("192.168.124.1", f2.get("ipv4_src_addr"));
        assertEquals("239.255.255.250", f2.get("ipv4_dst_addr"));
        assertEquals(3072, f2.get("l4_src_port"));
        assertEquals(1900, f2.get("l4_dst_port"));
        assertEquals((short) 17, f2.get("protocol"));

        NetFlowV9Packet p3 = NetFlowV9Parser.parsePacket(Unpooled.wrappedBuffer(b3), cache, typeRegistry);
        assertEquals(1, p3.records().size());
    }

    @Test
    public void pcap_softflowd_NetFlowV9() throws Exception {
        final List<NetFlowV9BaseRecord> allRecords = new ArrayList<>();
        final List<NetFlowV9Template> allTemplates = new ArrayList<>();
        try (InputStream inputStream = Resources.getResource("netflow-data/netflow9.pcap").openStream()) {
            final Pcap pcap = Pcap.openStream(inputStream);
            pcap.loop(packet -> {
                        if (packet.hasProtocol(Protocol.UDP)) {
                            final UDPPacket udp = (UDPPacket) packet.getPacket(Protocol.UDP);
                            final ByteBuf byteBuf = Unpooled.wrappedBuffer(udp.getPayload().getArray());
                            final NetFlowV9Packet netFlowV9Packet = NetFlowV9Parser.parsePacket(byteBuf, cache, typeRegistry);
                            assertThat(netFlowV9Packet).isNotNull();
                            allTemplates.addAll(netFlowV9Packet.templates());
                            allRecords.addAll(netFlowV9Packet.records());
                        }
                        return true;
                    }
            );
        }
        assertThat(allTemplates).contains(
                NetFlowV9Template.create(1024, 13,
                        ImmutableList.<NetFlowV9FieldDef>builder().add(
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(8, NetFlowV9FieldType.ValueType.IPV4, "ipv4_src_addr"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(12, NetFlowV9FieldType.ValueType.IPV4, "ipv4_dst_addr"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(21, NetFlowV9FieldType.ValueType.UINT32, "last_switched"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(22, NetFlowV9FieldType.ValueType.UINT32, "first_switched"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(1, NetFlowV9FieldType.ValueType.UINT32, "in_bytes"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(2, NetFlowV9FieldType.ValueType.UINT32, "in_pkts"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(10, NetFlowV9FieldType.ValueType.UINT16, "input_snmp"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(14, NetFlowV9FieldType.ValueType.UINT16, "output_snmp"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(7, NetFlowV9FieldType.ValueType.UINT16, "l4_src_port"), 2),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(11, NetFlowV9FieldType.ValueType.UINT16, "l4_dst_port"), 2),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(4, NetFlowV9FieldType.ValueType.UINT8, "protocol"), 1),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(6, NetFlowV9FieldType.ValueType.UINT8, "tcp_flags"), 1),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(60, NetFlowV9FieldType.ValueType.UINT8, "ip_protocol_version"), 1)
                        ).build()
                )
        );
        assertThat(allRecords).hasSize(19)
                .contains(
                        NetFlowV9Record.create(
                                ImmutableMap.<String, Object>builder()
                                        .put("ipv4_src_addr", "8.8.8.8")
                                        .put("ipv4_dst_addr", "192.168.1.20")
                                        .put("last_switched", 208442L)
                                        .put("first_switched", 208442L)
                                        .put("in_bytes", 76L)
                                        .put("in_pkts", 1L)
                                        .put("input_snmp", 0L)
                                        .put("output_snmp", 0L)
                                        .put("l4_src_port", 53)
                                        .put("l4_dst_port", 34865)
                                        .put("protocol", (short) 17)
                                        .put("tcp_flags", (short) 0)
                                        .put("ip_protocol_version", (short) 4L)
                                        .build())
                );
        ;
    }

    @Test
    public void pcap_pmacctd_NetFlowV9() throws Exception {
        final List<NetFlowV9BaseRecord> allRecords = new ArrayList<>();
        final List<NetFlowV9Template> allTemplates = new ArrayList<>();
        try (InputStream inputStream = Resources.getResource("netflow-data/pmacctd-netflow9.pcap").openStream()) {
            final Pcap pcap = Pcap.openStream(inputStream);
            pcap.loop(packet -> {
                        if (packet.hasProtocol(Protocol.UDP)) {
                            final UDPPacket udp = (UDPPacket) packet.getPacket(Protocol.UDP);
                            final ByteBuf byteBuf = Unpooled.wrappedBuffer(udp.getPayload().getArray());
                            final NetFlowV9Packet netFlowV9Packet = NetFlowV9Parser.parsePacket(byteBuf, cache, typeRegistry);
                            assertThat(netFlowV9Packet).isNotNull();
                            allTemplates.addAll(netFlowV9Packet.templates());
                            allRecords.addAll(netFlowV9Packet.records());
                        }
                        return true;
                    }
            );
        }
        assertThat(allTemplates).contains(
                NetFlowV9Template.create(1024, 10,
                        ImmutableList.<NetFlowV9FieldDef>builder().add(
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(153, NetFlowV9FieldType.ValueType.UINT64, "nf_field_153"), 8),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(152, NetFlowV9FieldType.ValueType.UINT64, "flow_start_msec"), 8),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(1, NetFlowV9FieldType.ValueType.UINT32, "in_bytes"), 8),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(2, NetFlowV9FieldType.ValueType.UINT32, "in_pkts"), 8),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(60, NetFlowV9FieldType.ValueType.UINT8, "ip_protocol_version"), 1),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(10, NetFlowV9FieldType.ValueType.UINT16, "input_snmp"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(14, NetFlowV9FieldType.ValueType.UINT16, "output_snmp"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(61, NetFlowV9FieldType.ValueType.UINT8, "direction"), 1),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(8, NetFlowV9FieldType.ValueType.IPV4, "ipv4_src_addr"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(12, NetFlowV9FieldType.ValueType.IPV4, "ipv4_dst_addr"), 4)
                        ).build()
                )
        );
        assertThat(allRecords)
                .hasSize(6)
                .contains(
                        NetFlowV9Record.create(
                                ImmutableMap.<String, Object>builder()
                                        .put("nf_field_153", 1501508283491L)
                                        .put("flow_start_msec", 1501508283473L)
                                        .put("in_bytes", 68L)
                                        .put("in_pkts", 1L)
                                        .put("ip_protocol_version", (short) 4)
                                        .put("input_snmp", 0L)
                                        .put("output_snmp", 0L)
                                        .put("direction", (short) 0)
                                        .put("ipv4_src_addr", "172.17.0.2")
                                        .put("ipv4_dst_addr", "8.8.4.4")
                                        .build())
                );
    }

    @Test
    public void pcap_nprobe_NetFlowV9_2() throws Exception {
        final List<NetFlowV9BaseRecord> allRecords = new ArrayList<>();
        final List<NetFlowV9Template> allTemplates = new ArrayList<>();
        try (InputStream inputStream = Resources.getResource("netflow-data/nprobe-netflow9-2.pcap").openStream()) {
            final Pcap pcap = Pcap.openStream(inputStream);
            pcap.loop(packet -> {
                        if (packet.hasProtocol(Protocol.UDP)) {
                            final UDPPacket udp = (UDPPacket) packet.getPacket(Protocol.UDP);
                            final ByteBuf byteBuf = Unpooled.wrappedBuffer(udp.getPayload().getArray());
                            final NetFlowV9Packet netFlowV9Packet = NetFlowV9Parser.parsePacket(byteBuf, cache, typeRegistry);
                            assertThat(netFlowV9Packet).isNotNull();
                            allTemplates.addAll(netFlowV9Packet.templates());
                            allRecords.addAll(netFlowV9Packet.records());
                        }
                        return true;
                    }
            );
        }
        assertThat(allTemplates).contains(
                NetFlowV9Template.create(257, 18,
                        ImmutableList.<NetFlowV9FieldDef>builder().add(
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(1, NetFlowV9FieldType.ValueType.UINT32, "in_bytes"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(2, NetFlowV9FieldType.ValueType.UINT32, "in_pkts"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(4, NetFlowV9FieldType.ValueType.UINT8, "protocol"), 1),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(5, NetFlowV9FieldType.ValueType.UINT8, "src_tos"), 1),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(6, NetFlowV9FieldType.ValueType.UINT8, "tcp_flags"), 1),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(7, NetFlowV9FieldType.ValueType.UINT16, "l4_src_port"), 2),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(8, NetFlowV9FieldType.ValueType.IPV4, "ipv4_src_addr"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(9, NetFlowV9FieldType.ValueType.UINT8, "src_mask"), 1),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(10, NetFlowV9FieldType.ValueType.UINT16, "input_snmp"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(11, NetFlowV9FieldType.ValueType.UINT16, "l4_dst_port"), 2),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(12, NetFlowV9FieldType.ValueType.IPV4, "ipv4_dst_addr"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(13, NetFlowV9FieldType.ValueType.UINT8, "dst_mask"), 1),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(14, NetFlowV9FieldType.ValueType.UINT16, "output_snmp"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(15, NetFlowV9FieldType.ValueType.IPV4, "ipv4_next_hop"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(16, NetFlowV9FieldType.ValueType.UINT16, "src_as"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(17, NetFlowV9FieldType.ValueType.UINT16, "dst_as"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(21, NetFlowV9FieldType.ValueType.UINT32, "last_switched"), 4),
                                NetFlowV9FieldDef.create(NetFlowV9FieldType.create(22, NetFlowV9FieldType.ValueType.UINT32, "first_switched"), 4)
                        ).build()
                )
        );
        assertThat(allRecords)
                .hasSize(7)
                .contains(
                        NetFlowV9Record.create(
                                ImmutableMap.<String, Object>builder()
                                        .put("in_bytes", 375L)
                                        .put("in_pkts", 7L)
                                        .put("ipv4_src_addr", "172.17.0.2")
                                        .put("ipv4_dst_addr", "93.184.216.34")
                                        .put("ipv4_next_hop", "0.0.0.0")
                                        .put("l4_src_port", 43296)
                                        .put("l4_dst_port", 80)
                                        .put("protocol", (short) 6)
                                        .put("src_tos", (short) 0)
                                        .put("tcp_flags", (short) 27)
                                        .put("src_mask", (short) 0)
                                        .put("dst_mask", (short) 0)
                                        .put("input_snmp", 0L)
                                        .put("output_snmp", 0L)
                                        .put("src_as", 0L)
                                        .put("dst_as", 15133L)
                                        .put("first_switched", 3L)
                                        .put("last_switched", 413L)
                                        .build())
                        ,
                        NetFlowV9Record.create(
                                ImmutableMap.<String, Object>builder()
                                        .put("in_bytes", 1829L)
                                        .put("in_pkts", 6L)
                                        .put("ipv4_src_addr", "93.184.216.34")
                                        .put("ipv4_dst_addr", "172.17.0.2")
                                        .put("ipv4_next_hop", "0.0.0.0")
                                        .put("l4_src_port", 80)
                                        .put("l4_dst_port", 43296)
                                        .put("protocol", (short) 6)
                                        .put("src_tos", (short) 0)
                                        .put("tcp_flags", (short) 27)
                                        .put("src_mask", (short) 0)
                                        .put("dst_mask", (short) 0)
                                        .put("input_snmp", 0L)
                                        .put("output_snmp", 0L)
                                        .put("src_as", 15133L)
                                        .put("dst_as", 0L)
                                        .put("first_switched", 138L)
                                        .put("last_switched", 413L)
                                        .build()),
                        NetFlowV9Record.create(
                                ImmutableMap.<String, Object>builder()
                                        .put("in_bytes", 68L)
                                        .put("in_pkts", 1L)
                                        .put("ipv4_src_addr", "172.17.0.2")
                                        .put("ipv4_dst_addr", "8.8.4.4")
                                        .put("ipv4_next_hop", "0.0.0.0")
                                        .put("l4_src_port", 60546)
                                        .put("l4_dst_port", 53)
                                        .put("protocol", (short) 17)
                                        .put("src_tos", (short) 0)
                                        .put("tcp_flags", (short) 0)
                                        .put("src_mask", (short) 0)
                                        .put("dst_mask", (short) 0)
                                        .put("input_snmp", 0L)
                                        .put("output_snmp", 0L)
                                        .put("src_as", 0L)
                                        .put("dst_as", 15169L)
                                        .put("first_switched", 284L)
                                        .put("last_switched", 284L)
                                        .build()),
                        NetFlowV9Record.create(
                                ImmutableMap.<String, Object>builder()
                                        .put("in_bytes", 84L)
                                        .put("in_pkts", 1L)
                                        .put("ipv4_src_addr", "8.8.4.4")
                                        .put("ipv4_dst_addr", "172.17.0.2")
                                        .put("ipv4_next_hop", "0.0.0.0")
                                        .put("l4_src_port", 53)
                                        .put("l4_dst_port", 60546)
                                        .put("protocol", (short) 17)
                                        .put("src_tos", (short) 0)
                                        .put("tcp_flags", (short) 0)
                                        .put("src_mask", (short) 0)
                                        .put("dst_mask", (short) 0)
                                        .put("input_snmp", 0L)
                                        .put("output_snmp", 0L)
                                        .put("src_as", 15169L)
                                        .put("dst_as", 0L)
                                        .put("first_switched", 321L)
                                        .put("last_switched", 321L)
                                        .build())
                );
    }

    private String name(NetFlowV9FieldDef def) {
        return def.type().name().toLowerCase();
    }
}
