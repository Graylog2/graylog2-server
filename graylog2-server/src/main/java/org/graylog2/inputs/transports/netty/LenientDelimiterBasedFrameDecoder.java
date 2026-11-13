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
/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.graylog2.inputs.transports.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;

import java.util.List;

/**
 * A {@link DelimiterBasedFrameDecoder} that optionally emits the remaining bytes as the last frame
 * when the channel is closed, even if they are not terminated by a delimiter.
 */
public class LenientDelimiterBasedFrameDecoder extends DelimiterBasedFrameDecoder {
    private final boolean emitLastLineWithoutDelimiter;
    /**
     * Set only when decoding with "\n" and "\r\n" as the delimiter. {@link DelimiterBasedFrameDecoder} only
     * delegates to {@link io.netty.handler.codec.LineBasedFrameDecoder} if it isn't subclassed, so we do it here.
     */
    private final LenientLineBasedFrameDecoder lineBasedDecoder;

    public LenientDelimiterBasedFrameDecoder(int maxFrameLength, ByteBuf delimiter) {
        this(maxFrameLength, true, delimiter);
    }

    public LenientDelimiterBasedFrameDecoder(int maxFrameLength, boolean stripDelimiter, ByteBuf delimiter) {
        this(maxFrameLength, stripDelimiter, true, delimiter);
    }

    public LenientDelimiterBasedFrameDecoder(int maxFrameLength, boolean stripDelimiter, boolean failFast, ByteBuf delimiter) {
        this(maxFrameLength, stripDelimiter, failFast, true, delimiter.slice(delimiter.readerIndex(), delimiter.readableBytes()));
    }

    public LenientDelimiterBasedFrameDecoder(int maxFrameLength, ByteBuf... delimiters) {
        this(maxFrameLength, true, delimiters);
    }

    public LenientDelimiterBasedFrameDecoder(int maxFrameLength, boolean stripDelimiter, ByteBuf... delimiters) {
        this(maxFrameLength, stripDelimiter, true, true, delimiters);
    }

    public LenientDelimiterBasedFrameDecoder(int maxFrameLength, boolean stripDelimiter, boolean failFast,
                                             boolean emitLastLineWithoutDelimiter, ByteBuf... delimiters) {
        super(maxFrameLength, stripDelimiter, failFast, delimiters);
        this.emitLastLineWithoutDelimiter = emitLastLineWithoutDelimiter;
        this.lineBasedDecoder = isLineBased(delimiters)
                ? new LenientLineBasedFrameDecoder(maxFrameLength, stripDelimiter, failFast, false)
                : null;
    }

    /**
     * Returns true if the delimiters are "\n" and "\r\n".
     * <p>
     * Copied from the private {@code DelimiterBasedFrameDecoder#isLineBased(ByteBuf[])} in Netty 4.2.19.Final.
     */
    private static boolean isLineBased(final ByteBuf[] delimiters) {
        if (delimiters.length != 2) {
            return false;
        }
        ByteBuf a = delimiters[0];
        ByteBuf b = delimiters[1];
        if (a.capacity() < b.capacity()) {
            a = delimiters[1];
            b = delimiters[0];
        }
        return a.capacity() == 2 && b.capacity() == 1
                && a.getByte(0) == '\r' && a.getByte(1) == '\n'
                && b.getByte(0) == '\n';
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        if (lineBasedDecoder != null) {
            return lineBasedDecoder.decode(ctx, buffer);
        }
        return super.decode(ctx, buffer);
    }

    @Override
    protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        super.decodeLast(ctx, in, out);
        // Anything left is an unterminated frame within maxFrameLength. Longer frames have already been discarded.
        if (emitLastLineWithoutDelimiter && in.isReadable()) {
            out.add(in.readRetainedSlice(in.readableBytes()));
        }
    }
}
