package org.graylog.plugins.netflow.flows;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import io.netty.buffer.Unpooled;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URL;

import static org.joda.time.DateTimeZone.UTC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class NetFlowV5PacketTest {
    @Test
    public void test() throws Exception {
        final URL resource = Resources.getResource("netflow-data/netflow-v5-1.dat");
        final byte[] bytes = Resources.toByteArray(resource);

        final NetFlowV5Packet packet = NetFlowV5Packet.parse(new InetSocketAddress("127.0.0.2", 2055), Unpooled.wrappedBuffer(bytes));

        final NetFlow flow1 = Lists.newArrayList(packet.getFlows()).get(0);
        final NetFlow flow2 = Lists.newArrayList(packet.getFlows()).get(1);
        final Message message1 = flow1.toMessage();
        final Message message2 = flow2.toMessage();

        assertEquals(2, packet.getFlows().size());
        //assertEquals(0, packet.engineId);
        //assertEquals(0, packet.engineType);

        assertEquals("127.0.0.2", message1.getSource());
        assertEquals(5, message1.getField("nf_version"));
        assertNotNull(message1.getField("nf_id"));
        assertNotNull(message1.getField("nf_flow_packet_id"));
        assertNotEquals(message1.getField("nf_id"), message1.getField("nf_flow_packet_id"));
        assertEquals(0, message1.getField("nf_tos"));
        assertEquals("10.0.2.2", message1.getField("nf_src_address"));
        assertEquals("10.0.2.15", message1.getField("nf_dst_address"));
        assertEquals("0.0.0.0", message1.getField("nf_next_hop"));
        assertEquals(54435, message1.getField("nf_src_port"));
        assertEquals(22, message1.getField("nf_dst_port"));
        assertEquals(0, message1.getField("nf_src_mask"));
        assertEquals(0, message1.getField("nf_dst_mask"));
        assertEquals(6, message1.getField("nf_proto"));
        assertEquals(16, message1.getField("nf_tcp_flags"));
        assertEquals(new DateTime("2015-06-21T13:40:51.914+02:00", UTC), message1.getField("nf_start"));
        assertEquals(new DateTime("2015-05-02T18:38:07.196Z", UTC), message1.getField("nf_stop"));
        assertEquals(230L, message1.getField("nf_bytes"));
        assertEquals(5L, message1.getField("nf_pkts"));

        assertEquals("127.0.0.2", message1.getSource());
        assertEquals(5, message2.getField("nf_version"));
        assertNotNull(message2.getField("nf_id"));
        assertNotNull(message2.getField("nf_flow_packet_id"));
        assertNotEquals(message2.getField("nf_id"), message2.getField("nf_flow_packet_id"));
        assertEquals(0, message2.getField("nf_tos"));
        assertEquals("10.0.2.15", message2.getField("nf_src_address"));
        assertEquals("10.0.2.2", message2.getField("nf_dst_address"));
        assertEquals("0.0.0.0", message2.getField("nf_next_hop"));
        assertEquals(22, message2.getField("nf_src_port"));
        assertEquals(54435, message2.getField("nf_dst_port"));
        assertEquals(0, message2.getField("nf_src_mask"));
        assertEquals(0, message2.getField("nf_dst_mask"));
        assertEquals(6, message2.getField("nf_proto"));
        assertEquals(24, message2.getField("nf_tcp_flags"));
        assertEquals(new DateTime("2015-06-21T13:40:51.914+02:00", UTC), message2.getField("nf_start"));
        assertEquals(new DateTime("2015-05-02T18:38:07.196Z", UTC), message2.getField("nf_stop"));
        assertEquals(304L, message2.getField("nf_bytes"));
        assertEquals(4L, message2.getField("nf_pkts"));
    }
}