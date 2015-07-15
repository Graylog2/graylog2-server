package org.graylog.plugins.netflow.flows.cflow;

import com.google.common.io.Resources;
import io.netty.buffer.Unpooled;
import org.joda.time.DateTime;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertEquals;

public class NetFlowV5PacketTest {
    @Test
    public void test() throws Exception {
        final URL resource = Resources.getResource("netflow-data/netflow-v5-1.dat");
        final byte[] bytes = Resources.toByteArray(resource);

        final NetFlowV5Packet packet = NetFlowV5Packet.parse(null, Unpooled.wrappedBuffer(bytes));

        final NetFlowV5 flow1 = packet.flows.get(0);
        final NetFlowV5 flow2 = packet.flows.get(1);

        assertEquals(2, packet.flows.size());
        assertEquals(0, packet.engineId);
        assertEquals(0, packet.engineType);

        assertEquals(0, flow1.tos);
        assertEquals("/10.0.2.2", flow1.srcAddress.toString());
        assertEquals("/10.0.2.15", flow1.dstAddress.toString());
        assertEquals("/0.0.0.0", flow1.nextHop.get().toString());
        assertEquals(54435, flow1.srcPort);
        assertEquals(22, flow1.dstPort);
        assertEquals(6, flow1.proto);
        assertEquals(16, flow1.tcpflags);
        assertEquals(new DateTime("2015-06-21T13:40:51.914+02:00"), flow1.start.get());
        assertEquals(new DateTime("2015-05-02T18:38:07.196Z"), flow1.stop.get());
        assertEquals(230, flow1.bytes);
        assertEquals(5, flow1.pkts);
    }
}