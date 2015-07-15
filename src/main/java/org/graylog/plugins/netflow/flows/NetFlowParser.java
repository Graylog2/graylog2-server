package org.graylog.plugins.netflow.flows;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.graylog.plugins.netflow.flows.cflow.NetFlowV5Packet;
import org.graylog2.plugin.journal.RawMessage;

import java.net.InetSocketAddress;

public class NetFlowParser {
    public static NetFlowV5Packet parse(RawMessage rawMessage) throws FlowException {
        final InetSocketAddress sender = rawMessage.getRemoteAddress() != null ? rawMessage.getRemoteAddress().getInetSocketAddress() : null;
        final ByteBuf buf = Unpooled.wrappedBuffer(rawMessage.getPayload());

        switch (buf.getUnsignedShort(0)) {
            case 5:
                return NetFlowV5Packet.parse(sender, buf);
            default:
                return null;
        }
    }
}
