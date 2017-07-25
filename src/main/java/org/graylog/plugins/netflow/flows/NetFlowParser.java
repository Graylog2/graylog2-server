/**
 * Copyright (C) 2012, 2013, 2014 wasted.io Ltd <really@wasted.io>
 * Copyright (C) 2015-2017 Graylog, Inc. (hello@graylog.org)
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

package org.graylog.plugins.netflow.flows;

import io.netty.buffer.Unpooled;
import org.graylog.plugins.netflow.v5.NetFlowV5Packet;
import org.graylog.plugins.netflow.v5.NetFlowV5Parser;
import org.graylog.plugins.netflow.v9.NetFlowV9FieldTypeRegistry;
import org.graylog.plugins.netflow.v9.NetFlowV9Packet;
import org.graylog.plugins.netflow.v9.NetFlowV9Parser;
import org.graylog.plugins.netflow.v9.NetFlowV9Record;
import org.graylog.plugins.netflow.v9.NetFlowV9TemplateCache;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ResolvableInetSocketAddress;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

public class NetFlowParser {
    private static final Logger LOG = LoggerFactory.getLogger(NetFlowParser.class);

    @Nullable
    public static List<Message> parse(RawMessage rawMessage, NetFlowV9TemplateCache templateCache, NetFlowV9FieldTypeRegistry typeRegistry) throws FlowException {
        final ResolvableInetSocketAddress remoteAddress = rawMessage.getRemoteAddress();
        final InetSocketAddress sender = remoteAddress != null ? remoteAddress.getInetSocketAddress() : null;

        final byte[] payload = rawMessage.getPayload();
        if(payload.length < 2) {
            LOG.debug("NetFlow message (source: {}) doesn't even fit the NetFlow version (size: {} bytes)",
                    sender, payload.length);
            return null;
        }

        final int netFlowVersion = (payload[0] << 8) + payload[1];
        switch (netFlowVersion) {
            case 5:
                final NetFlowV5Packet netFlowV5Packet = NetFlowV5Parser.parsePacket(Unpooled.wrappedBuffer(payload));

                return netFlowV5Packet.records().stream()
                        .map(record ->  NetFlowFormatter.toMessage(netFlowV5Packet.header(), record, sender))
                        .collect(Collectors.toList());
            case 9:
                final NetFlowV9Packet netFlowV9Packet = NetFlowV9Parser.parsePacket(Unpooled.wrappedBuffer(payload), templateCache, typeRegistry);
                return netFlowV9Packet.records().stream()
                        .filter(record -> record instanceof NetFlowV9Record)
                    .map(record ->  NetFlowFormatter.toMessage(netFlowV9Packet.header(), record, sender))
                    .collect(Collectors.toList());
            default:
                final List<RawMessage.SourceNode> sourceNodes = rawMessage.getSourceNodes();
                final RawMessage.SourceNode sourceNode = sourceNodes.isEmpty() ? null : sourceNodes.get(sourceNodes.size() - 1);
                final String inputId = sourceNode == null ? "<unknown>" : sourceNode.inputId;
                LOG.warn("Unsupported NetFlow version {} on input {} (source: {})", netFlowVersion, inputId, sender);
                return null;
        }
    }
}
