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

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import jakarta.inject.Inject;
import opamp.proto.Opamp.AgentToServer;
import opamp.proto.Opamp.ServerToAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpAMPFrameHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {
    private static final Logger LOG = LoggerFactory.getLogger(OpAMPFrameHandler.class);

    @Inject
    public OpAMPFrameHandler() {
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
        }
    }

    private ServerToAgent processMessage(AgentToServer message) {
        // TODO: Implement full message processing
        // For now, return a basic acknowledgment response
        return ServerToAgent.newBuilder()
                .setInstanceUid(message.getInstanceUid())
                .build();
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
