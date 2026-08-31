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
package org.graylog.plugins.netflow.v9;

import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import io.netty.buffer.Unpooled;
import org.graylog.plugins.netflow.flows.CorruptFlowPacketException;
import org.graylog.plugins.netflow.flows.EmptyTemplateException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NetFlowV9ParserTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private NetFlowV9FieldTypeRegistry typeRegistry;

    @Before
    public void setUp() throws IOException {
        typeRegistry = NetFlowV9FieldTypeRegistry.create();

    }

    @Test
    public void testParse() throws IOException {
        final byte[] b1 = Resources.toByteArray(Resources.getResource("netflow-data/netflow-v9-2-1.dat"));
        final byte[] b2 = Resources.toByteArray(Resources.getResource("netflow-data/netflow-v9-2-2.dat"));
        final byte[] b3 = Resources.toByteArray(Resources.getResource("netflow-data/netflow-v9-2-3.dat"));

        Map<Integer, NetFlowV9Template> cache = Maps.newHashMap();
        // check header
        NetFlowV9Packet p1 = NetFlowV9Parser.parsePacket(Unpooled.wrappedBuffer(b1), typeRegistry, cache, null);
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

        NetFlowV9Packet p2 = NetFlowV9Parser.parsePacket(Unpooled.wrappedBuffer(b2), typeRegistry, cache, null);
        NetFlowV9BaseRecord r2 = p2.records().get(0);
        Map<String, Object> f2 = r2.fields();
        assertEquals(2818L, f2.get("in_bytes"));
        assertEquals(8L, f2.get("in_pkts"));
        assertEquals("192.168.124.1", f2.get("ipv4_src_addr"));
        assertEquals("239.255.255.250", f2.get("ipv4_dst_addr"));
        assertEquals(3072, f2.get("l4_src_port"));
        assertEquals(1900, f2.get("l4_dst_port"));
        assertEquals((short) 17, f2.get("protocol"));

        NetFlowV9Packet p3 = NetFlowV9Parser.parsePacket(Unpooled.wrappedBuffer(b3), typeRegistry, cache, null);
        assertEquals(1, p3.records().size());
    }

    @Test
    public void testParseIncomplete() throws Exception {
        final byte[] b = Resources.toByteArray(Resources.getResource("netflow-data/netflow-v9-3_incomplete.dat"));
        assertThatExceptionOfType(EmptyTemplateException.class)
                .isThrownBy(() -> NetFlowV9Parser.parsePacket(Unpooled.wrappedBuffer(b), typeRegistry));
    }

    // Each case below used to rewind the reader index to the start of the FlowSet and spin forever.
    // The timeouts fail the test rather than hanging the suite if that regresses.

    // Only the version is validated, so sysUptime / unixSecs / sequence / sourceId are left zero.
    private static final String HEADER_ONE_FLOWSET = "0009" + "0001" + "00000000" + "00000000" + "00000000" + "00000000";
    private static final String HEADER_TWO_FLOWSETS = "0009" + "0002" + "00000000" + "00000000" + "00000000" + "00000000";

    @Test(timeout = 5000)
    public void parsePacketShallow_dataFlowSetWithZeroLength_isRejected() {
        // FlowSet id 256 (any id above 1 is a data FlowSet), length 0
        final byte[] b = hexToBytes(HEADER_ONE_FLOWSET + "0100" + "0000");

        assertThatExceptionOfType(CorruptFlowPacketException.class)
                .isThrownBy(() -> NetFlowV9Parser.parsePacketShallow(Unpooled.wrappedBuffer(b)));
    }

    @Test(timeout = 5000)
    public void parsePacketShallow_optionTemplateWithZeroLength_isRejected() {
        // FlowSet id 1 (options template), length 0, then template id and zero scope/option lengths
        final byte[] b = hexToBytes(HEADER_ONE_FLOWSET + "0001" + "0000" + "0100" + "0000" + "0000");

        assertThatExceptionOfType(CorruptFlowPacketException.class)
                .isThrownBy(() -> NetFlowV9Parser.parsePacketShallow(Unpooled.wrappedBuffer(b)));
    }

    @Test(timeout = 5000)
    public void parsePacketShallow_dataFlowSetWithHeaderOnlyLength_parsesCleanly() {
        // Same packet except length 4: a FlowSet header and no records, which must still parse.
        final byte[] b = hexToBytes(HEADER_ONE_FLOWSET + "0100" + "0004");

        final RawNetFlowV9Packet packet = NetFlowV9Parser.parsePacketShallow(Unpooled.wrappedBuffer(b));

        assertEquals(9, packet.header().version());
        assertEquals(Collections.singleton(256), packet.usedTemplates());
    }

    @Test(timeout = 5000)
    public void parsePacket_dataFlowSetWithZeroLength_isRejected() {
        // The template for id 256 is needed so the record parser gets past its cache lookup.
        final byte[] b = hexToBytes(HEADER_TWO_FLOWSETS
                + "0000" + "0010" + "0100" + "0002" + "0008" + "0004" + "000c" + "0004"
                + "0100" + "0000");

        assertThatExceptionOfType(CorruptFlowPacketException.class)
                .isThrownBy(() -> NetFlowV9Parser.parsePacket(Unpooled.wrappedBuffer(b), typeRegistry));
    }

    @Test(timeout = 5000)
    public void parsePacket_optionTemplateWithZeroLength_isRejected() {
        final byte[] b = hexToBytes(HEADER_ONE_FLOWSET + "0001" + "0000" + "0100" + "0000" + "0000");

        assertThatExceptionOfType(CorruptFlowPacketException.class)
                .isThrownBy(() -> NetFlowV9Parser.parsePacket(Unpooled.wrappedBuffer(b), typeRegistry));
    }

    @Test(timeout = 5000)
    public void parsePacket_templateWithNoFields_isRejected() {
        // Distinct from the length cases: the FlowSet length here is legitimate, but a template
        // declaring zero fields makes each record occupy zero bytes, exhausting the heap.
        final byte[] b = hexToBytes(HEADER_TWO_FLOWSETS
                + "0000" + "0008" + "0100" + "0000"
                + "0100" + "0008" + "00000000");

        assertThatExceptionOfType(CorruptFlowPacketException.class)
                .isThrownBy(() -> NetFlowV9Parser.parsePacket(Unpooled.wrappedBuffer(b), typeRegistry));
    }

    private static byte[] hexToBytes(String hex) {
        final byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private String name(NetFlowV9FieldDef def) {
        return def.type().name().toLowerCase(Locale.ROOT);
    }
}
