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
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ByteProcessor;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Implements a Netty {@link ByteToMessageDecoder} for the Syslog octet counting framing. (RFC6587)
 *
 * @see <a href="http://tools.ietf.org/html/rfc6587#section-3.4.1">RFC6587 Octet Counting</a>
 */
public class SyslogOctetCountFrameDecoder extends ByteToMessageDecoder {
    private static final ByteProcessor BYTE_PROCESSOR = value -> value != ' ';

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
        final int frameSizeValueLength = findFrameSizeValueLength(buffer);

        // We have not found the frame length value byte size yet.
        if (frameSizeValueLength <= 0) {
            return;
        }

        // Convert the frame length value bytes into an integer without mutating the buffer reader index.
        final String lengthString = buffer.slice(buffer.readerIndex(), frameSizeValueLength).toString(StandardCharsets.UTF_8);
        final int length = Integer.parseInt(lengthString);
        final int skipLength = frameSizeValueLength + 1; // Frame length value bytes and the whitespace that follows it.

        // We have to take the skipped bytes (frame size value length + whitespace) into account when checking if
        // the buffer has enough data to read the complete message.
        if (buffer.readableBytes() - skipLength < length) {
            // We cannot read the complete frame yet.
            return;
        } else {
            // Skip the frame length value bytes and the whitespace that follows it.
            buffer.skipBytes(skipLength);
        }

        final ByteBuf frame = buffer.readRetainedSlice(length);
        out.add(frame);
    }

    /**
     * Find the byte length of the frame length value.
     *
     * @param buffer The channel buffer
     * @return The length of the frame length value
     */
    private int findFrameSizeValueLength(final ByteBuf buffer) {
        final int readerIndex = buffer.readerIndex();
        int index = buffer.forEachByte(BYTE_PROCESSOR);

        if (index >= 0) {
            return index - readerIndex;
        } else {
            return -1;
        }
    }
}
