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
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public class EnvelopeMessageHandler extends SimpleChannelInboundHandler<AddressedEnvelope<ByteBuf, InetSocketAddress>> {
    private static final Logger LOG = LoggerFactory.getLogger(NettyTransport.class);
    private static final Charset CHARSET = Charset.defaultCharset();

    private final MessageInput input;
    private final boolean trim;

    public EnvelopeMessageHandler(MessageInput input, boolean trim) {
        this.input = input;
        this.trim = trim;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AddressedEnvelope<ByteBuf, InetSocketAddress> envelope) throws Exception {
        final ByteBuf msg = envelope.content();
        final byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);
        final RawMessage raw;
        if (trim) {
            raw = new RawMessage(new String(bytes, CHARSET).trim().getBytes(CHARSET), envelope.sender());
        } else {
            raw = new RawMessage(bytes, envelope.sender());
        }
        input.processRawMessage(raw);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.debug("Could not handle message, closing connection: {}", cause);
        ctx.channel().close();
        super.exceptionCaught(ctx, cause);
    }
}
