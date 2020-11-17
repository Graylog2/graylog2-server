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
import org.graylog.plugins.netflow.flows.EmptyTemplateException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
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

    private String name(NetFlowV9FieldDef def) {
        return def.type().name().toLowerCase(Locale.ROOT);
    }
}
