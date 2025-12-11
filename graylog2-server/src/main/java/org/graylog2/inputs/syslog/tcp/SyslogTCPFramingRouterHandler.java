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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;

public class SyslogTCPFramingRouterHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger LOG = LoggerFactory.getLogger(SyslogTCPFramingRouterHandler.class);
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
        LOG.warn("RefCnt of msg entering channelRead0: {}", msg.refCnt());
        if (msg.isReadable()) {
            LOG.info("Is readable.");
            // "Dynamically manipulating a pipeline is relatively an expensive operation."
            // https://stackoverflow.com/a/28846565
            if (handler == null) {
                if (usesOctetCountFraming(msg)) {
                    LOG.debug("Uses Octet Count Framing");
                    handler = new SyslogOctetCountFrameDecoder();
                } else {
                    LOG.debug("Uses delimiter based Framing with max length {} and delimiter {}", maxFrameLength, delimiter);
                    handler = new DelimiterBasedFrameDecoder(maxFrameLength, delimiter);
                }
            }
            LOG.info("Passing on to handler (refCnt {}), ctx {}", msg.refCnt(), ctx);
            // Retain message for possible next frame.
            retainedBuffers.add(msg.retain());
            handler.channelRead(ctx, msg);
            LOG.info("After handler.channelRead(): refCnt {}.", msg.refCnt());
        } else {
            LOG.info("msg not readable (refCnt {}).", msg.refCnt());
            ctx.fireChannelRead(msg);
        }
        LOG.warn("RefCnt of msg leaving channelRead0: {}", msg.refCnt());
    }

    private void cleanup(final String from) {
        LOG.info("{}: Releasing {} retained buffers.", from, retainedBuffers.size());
        for (ByteBuf buf : retainedBuffers) {
            if (buf.refCnt() > 0) {
                LOG.info("Releasing buffer with refCnt {}.", buf.refCnt());
                buf.release();
            } else {
                LOG.info("buf has a refCnt of 0 already.");
            }

        }
        retainedBuffers.clear();
        LOG.info("Done cleaning up (after {}).", from);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        cleanup("channelInactive");
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cleanup("exception");
        super.exceptionCaught(ctx, cause);
    }

    private boolean usesOctetCountFraming(ByteBuf message) {
        // Octet counting framing needs to start with a non-zero digit.
        // See: http://tools.ietf.org/html/rfc6587#section-3.4.1
        final byte firstByte = message.getByte(0);
        return '0' < firstByte && firstByte <= '9';
    }
}
