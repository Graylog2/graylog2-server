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
package org.graylog2.inputs.syslog.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyslogTCPFramingRouterHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger LOG = LoggerFactory.getLogger(SyslogTCPFramingRouterHandler.class);

    private final int maxFrameLength;
    private final ByteBuf[] delimiter;
    private ChannelInboundHandler handler = null;

    public SyslogTCPFramingRouterHandler(int maxFrameLength, ByteBuf[] delimiter) {
        super(false); // Disable auto-release — downstream handler owns the buffer
        this.maxFrameLength = maxFrameLength;
        this.delimiter = delimiter;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (msg.isReadable()) {
            // "Dynamically manipulating a pipeline is relatively an expensive operation."
            // https://stackoverflow.com/a/28846565
            if (handler == null) {
                if (usesOctetCountFraming(msg)) {
                    handler = new SyslogOctetCountFrameDecoder();
                } else {
                    handler = new DelimiterBasedFrameDecoder(maxFrameLength, delimiter);
                }
            }
            handler.channelRead(ctx, msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (handler != null) {
            try {
                // Forward lifecycle event so ByteToMessageDecoder releases its internal
                // cumulation buffer. This may throw if the remaining data cannot be decoded
                // (e.g. incomplete syslog frame at connection close), but the cumulation is
                // released in the finally block of ByteToMessageDecoder.channelInputClosed
                // regardless. That method also fires channelInactive downstream.
                handler.channelInactive(ctx);
            } catch (DecoderException e) {
                LOG.debug("Failed to decode remaining data on channel close", e);
            }
        } else {
            super.channelInactive(ctx);
        }
    }

    private boolean usesOctetCountFraming(ByteBuf message) {
        // Octet counting framing needs to start with a non-zero digit.
        // See: http://tools.ietf.org/html/rfc6587#section-3.4.1
        final byte firstByte = message.getByte(0);
        return '0' < firstByte && firstByte <= '9';
    }
}
