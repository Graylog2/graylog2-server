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
import io.netty.handler.codec.DelimiterBasedFrameDecoder;

import java.util.Collection;
import java.util.LinkedList;

public class SyslogTCPFramingRouterHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final int maxFrameLength;
    private final ByteBuf[] delimiter;
    private final Collection<ByteBuf> retainedBuffers = new LinkedList<>();
    private ChannelInboundHandler handler = null;

    public SyslogTCPFramingRouterHandler(int maxFrameLength, ByteBuf[] delimiter) {
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
            // Retain message for possible next frame.
            retainedBuffers.add(msg.retain());
            handler.channelRead(ctx, msg);
        } else {
            ctx.fireChannelRead(msg);
        }

        // In case the TCP-connection is kept open for a long time, and there's some traffic happening,
        // we can skim through the retained buffers we keep track of and forget the ones that are freed already.
        // This does not help for long, idle connections and gigantic frames.
        retainedBuffers.removeIf(buf -> buf.refCnt() == 0);
    }

    private void cleanup() {
        for (ByteBuf buf : retainedBuffers) {
            if (buf.refCnt() > 0) {
                buf.release();
            }
        }
        retainedBuffers.clear();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        cleanup();
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cleanup();
        super.exceptionCaught(ctx, cause);
    }

    private boolean usesOctetCountFraming(ByteBuf message) {
        // Octet counting framing needs to start with a non-zero digit.
        // See: http://tools.ietf.org/html/rfc6587#section-3.4.1
        final byte firstByte = message.getByte(0);
        return '0' < firstByte && firstByte <= '9';
    }
}
