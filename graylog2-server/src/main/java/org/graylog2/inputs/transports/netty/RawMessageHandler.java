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
package org.graylog2.inputs.transports.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class RawMessageHandler extends SimpleChannelInboundHandler<ByteBuf> {
    /**
     * If another handler sets an attribute with this key, we will use its value as the remote address
     * if it's a valid InetSocketAddress
     */
    public static final AttributeKey<InetSocketAddress> ORIGINAL_IP_KEY =
            AttributeKey.valueOf("RawMessageHandler.ORIGINAL_IP");
    private static final Logger LOG = LoggerFactory.getLogger(NettyTransport.class);

    private final MessageInput input;

    public RawMessageHandler(MessageInput input) {
        this.input = input;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        final byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        if (ctx.channel().hasAttr(ORIGINAL_IP_KEY)) {
            remoteAddress = ctx.channel().attr(ORIGINAL_IP_KEY).get();
        }
        final RawMessage raw = new RawMessage(bytes, remoteAddress);
        input.processRawMessage(raw);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.debug("Could not handle message, closing connection.", cause);
        ctx.channel().close();
        super.exceptionCaught(ctx, cause);
    }
}
