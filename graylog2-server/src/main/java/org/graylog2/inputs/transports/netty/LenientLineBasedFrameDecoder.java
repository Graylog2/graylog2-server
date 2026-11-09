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
import io.netty.handler.codec.LineBasedFrameDecoder;

import java.util.List;

/**
 * A {@link LineBasedFrameDecoder} that optionally emits the remaining bytes as the last frame
 * when the channel is closed, even if they are not terminated by a line ending.
 */
public class LenientLineBasedFrameDecoder extends LineBasedFrameDecoder {
    private final boolean emitLastLineWithoutDelimiter;

    public LenientLineBasedFrameDecoder(final int maxLength) {
        this(maxLength, true, false, true);
    }

    public LenientLineBasedFrameDecoder(final int maxLength, final boolean stripDelimiter, final boolean failFast,
                                        final boolean emitLastLineWithoutDelimiter) {
        super(maxLength, stripDelimiter, failFast);
        this.emitLastLineWithoutDelimiter = emitLastLineWithoutDelimiter;
    }

    // Overridden to make it callable from LenientDelimiterBasedFrameDecoder in this package.
    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        return super.decode(ctx, buffer);
    }

    @Override
    protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        super.decodeLast(ctx, in, out);
        // Anything left is an unterminated frame within maxLength. Longer frames have already been discarded.
        if (emitLastLineWithoutDelimiter && in.isReadable()) {
            out.add(in.readRetainedSlice(in.readableBytes()));
        }
    }
}
