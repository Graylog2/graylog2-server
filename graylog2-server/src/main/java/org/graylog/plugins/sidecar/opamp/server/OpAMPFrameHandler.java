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
package org.graylog.plugins.sidecar.opamp.server;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import opamp.proto.Anyvalue.KeyValue;
import opamp.proto.Opamp.AgentDescription;
import opamp.proto.Opamp.AgentToServer;
import opamp.proto.Opamp.ServerToAgent;
import org.graylog.plugins.sidecar.rest.models.NodeDetails;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.graylog.plugins.sidecar.services.SidecarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HexFormat;
import java.util.Set;

@Singleton
@ChannelHandler.Sharable
public class OpAMPFrameHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {
    private static final Logger LOG = LoggerFactory.getLogger(OpAMPFrameHandler.class);

    private final SidecarService sidecarService;

    @Inject
    public OpAMPFrameHandler(SidecarService sidecarService) {
        this.sidecarService = sidecarService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        final ByteBuf content = frame.content();
        final byte[] bytes = new byte[content.readableBytes()];
        content.readBytes(bytes);

        try {
            final AgentToServer message = AgentToServer.parseFrom(bytes);
            LOG.debug("Received AgentToServer message from {}: {}", ctx.channel().remoteAddress(), message);

            // Process the message and build response
            final ServerToAgent response = processMessage(message);

            // Send response
            ctx.writeAndFlush(new BinaryWebSocketFrame(
                    Unpooled.wrappedBuffer(response.toByteArray())));

        } catch (InvalidProtocolBufferException e) {
            LOG.error("Failed to parse AgentToServer message", e);
            ctx.close();
        } catch (Exception e) {
            LOG.error("Error processing AgentToServer message", e);
            ctx.close();
        }
    }

    private ServerToAgent processMessage(AgentToServer message) {
        final String nodeId = extractNodeId(message.getInstanceUid());

        // Extract agent description and create/update sidecar
        if (message.hasAgentDescription()) {
            final Sidecar sidecar = createSidecarFromAgent(nodeId, message.getAgentDescription());
            sidecarService.save(sidecar);
            LOG.info("Registered/updated OpAMP agent: nodeId={}, nodeName={}", nodeId, sidecar.nodeName());
        } else {
            // Just update last_seen for existing sidecar
            final Sidecar existing = sidecarService.findByNodeId(nodeId);
            if (existing != null) {
                sidecarService.save(existing.toBuilder()
                        .lastSeen(org.joda.time.DateTime.now(org.joda.time.DateTimeZone.UTC))
                        .build());
                LOG.debug("Updated last_seen for OpAMP agent: nodeId={}", nodeId);
            } else {
                LOG.warn("Received message from unknown agent without agent_description: nodeId={}", nodeId);
            }
        }

        return ServerToAgent.newBuilder()
                .setInstanceUid(message.getInstanceUid())
                .build();
    }

    private Sidecar createSidecarFromAgent(String nodeId, AgentDescription agentDescription) {
        // Extract values from identifying and non-identifying attributes
        String nodeName = nodeId;  // default
        String operatingSystem = "unknown";
        String ip = null;
        String version = "opamp";

        // Process identifying attributes (service.name, service.version, etc.)
        for (KeyValue kv : agentDescription.getIdentifyingAttributesList()) {
            final String key = kv.getKey();
            final String value = getStringValue(kv);
            switch (key) {
                case "service.instance.id" -> nodeName = value;
                case "service.version" -> version = value;
            }
        }

        // Process non-identifying attributes (os.type, host.name, host.ip, etc.)
        for (KeyValue kv : agentDescription.getNonIdentifyingAttributesList()) {
            final String key = kv.getKey();
            final String value = getStringValue(kv);
            switch (key) {
                case "host.name" -> nodeName = value;
                case "os.type" -> operatingSystem = value;
                case "host.ip" -> ip = value;
            }
        }

        final NodeDetails nodeDetails = NodeDetails.create(
                operatingSystem,
                ip,
                null,  // metrics
                null,  // logFileList
                null,  // statusList
                Set.of(),  // tags
                null   // collectorConfigurationDirectory
        );

        return Sidecar.create(nodeId, nodeName, nodeDetails, version);
    }

    private String getStringValue(KeyValue kv) {
        if (kv.hasValue() && kv.getValue().hasStringValue()) {
            return kv.getValue().getStringValue();
        }
        return "";
    }

    private String extractNodeId(ByteString instanceUid) {
        // Convert the 16-byte UUID to a hex string for use as nodeId
        if (instanceUid.isEmpty()) {
            return "unknown-" + System.currentTimeMillis();
        }
        return HexFormat.of().formatHex(instanceUid.toByteArray());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOG.debug("OpAMP connection established from {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOG.debug("OpAMP connection closed from {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Error in OpAMP handler", cause);
        ctx.close();
    }
}
