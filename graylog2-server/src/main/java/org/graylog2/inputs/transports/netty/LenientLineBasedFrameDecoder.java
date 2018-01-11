/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.ByteProcessor;

import java.util.List;

/**
 * A decoder that splits the received {@link ByteBuf}s on line endings.
 * <p>
 * Both {@code "\n"} and {@code "\r\n"} are handled.
 * For a more general delimiter-based decoder, see {@link DelimiterBasedFrameDecoder}.
 */
public class LenientLineBasedFrameDecoder extends ByteToMessageDecoder {

    /**
     * Maximum length of a frame we're willing to decode.
     */
    private final int maxLength;
    /**
     * Whether or not to throw an exception as soon as we exceed maxLength.
     */
    private final boolean failFast;
    private final boolean stripDelimiter;
    private final boolean emitLastLineWithoutDelimiter;

    /**
     * True if we're discarding input because we're already over maxLength.
     */
    private boolean discarding;
    private int discardedBytes;

    /**
     * Last scan position.
     */
    private int offset;

    /**
     * Creates a new decoder.
     *
     * @param maxLength the maximum length of the decoded frame.
     *                  A {@link TooLongFrameException} is thrown if
     *                  the length of the frame exceeds this value.
     */
    public LenientLineBasedFrameDecoder(final int maxLength) {
        this(maxLength, true, false, true);
    }

    /**
     * Creates a new decoder.
     *
     * @param maxLength                    the maximum length of the decoded frame.
     *                                     A {@link TooLongFrameException} is thrown if
     *                                     the length of the frame exceeds this value.
     * @param stripDelimiter               whether the decoded frame should strip out the
     *                                     delimiter or not
     * @param failFast                     If <tt>true</tt>, a {@link TooLongFrameException} is
     *                                     thrown as soon as the decoder notices the length of the
     *                                     frame will exceed <tt>maxFrameLength</tt> regardless of
     *                                     whether the entire frame has been read.
     *                                     If <tt>false</tt>, a {@link TooLongFrameException} is
     *                                     thrown after the entire frame that exceeds
     *                                     <tt>maxFrameLength</tt> has been read.
     * @param emitLastLineWithoutDelimiter emit the last line even if it doesn't
     *                                     end with the delimiter
     */
    public LenientLineBasedFrameDecoder(final int maxLength, final boolean stripDelimiter, final boolean failFast,
                                        final boolean emitLastLineWithoutDelimiter) {
        this.maxLength = maxLength;
        this.failFast = failFast;
        this.stripDelimiter = stripDelimiter;
        this.emitLastLineWithoutDelimiter = emitLastLineWithoutDelimiter;
    }

    @Override
    protected final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Object decoded = decode(ctx, in);
        if (decoded != null) {
            out.add(decoded);
        }
    }

    /**
     * Create a frame out of the {@link ByteBuf} and return it.
     *
     * @param ctx    the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
     * @param buffer the {@link ByteBuf} from which to read data
     * @return frame           the {@link ByteBuf} which represent the frame or {@code null} if no frame could
     * be created.
     */
    protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        final int eol = findEndOfLine(buffer);
        if (!discarding) {
            if (eol < 0 && emitLastLineWithoutDelimiter && !ctx.channel().isActive()) {
                final ByteBuf frame;
                final int length = buffer.readableBytes();

                if (length > maxLength) {
                    buffer.readerIndex(length);
                    fail(ctx, length);
                    return null;
                }

                frame = buffer.readRetainedSlice(length);

                return frame;
            } else if (eol >= 0) {
                final ByteBuf frame;
                final int length = eol - buffer.readerIndex();
                final int delimLength = buffer.getByte(eol) == '\r' ? 2 : 1;

                if (length > maxLength) {
                    buffer.readerIndex(eol + delimLength);
                    fail(ctx, length);
                    return null;
                }

                if (stripDelimiter) {
                    frame = buffer.readRetainedSlice(length);
                    buffer.skipBytes(delimLength);
                } else {
                    frame = buffer.readRetainedSlice(length + delimLength);
                }

                return frame;
            } else {
                final int length = buffer.readableBytes();
                if (length > maxLength) {
                    discardedBytes = length;
                    buffer.readerIndex(buffer.writerIndex());
                    discarding = true;
                    offset = 0;
                    if (failFast) {
                        fail(ctx, "over " + discardedBytes);
                    }
                }
                return null;
            }
        } else {
            if (eol >= 0) {
                final int length = discardedBytes + eol - buffer.readerIndex();
                final int delimLength = buffer.getByte(eol) == '\r' ? 2 : 1;
                buffer.readerIndex(eol + delimLength);
                discardedBytes = 0;
                discarding = false;
                if (!failFast) {
                    fail(ctx, length);
                }
            } else {
                discardedBytes += buffer.readableBytes();
                buffer.readerIndex(buffer.writerIndex());
            }
            return null;
        }
    }

    private void fail(final ChannelHandlerContext ctx, int length) {
        fail(ctx, String.valueOf(length));
    }

    private void fail(final ChannelHandlerContext ctx, String length) {
        ctx.fireExceptionCaught(
                new TooLongFrameException(
                        "frame length (" + length + ") exceeds the allowed maximum (" + maxLength + ')'));
    }

    /**
     * Returns the index in the buffer of the end of line found.
     * Returns -1 if no end of line was found in the buffer.
     */
    private int findEndOfLine(final ByteBuf buffer) {
        int totalLength = buffer.readableBytes();
        int i = buffer.forEachByte(buffer.readerIndex() + offset, totalLength - offset, ByteProcessor.FIND_LF);
        if (i >= 0) {
            offset = 0;
            if (i > 0 && buffer.getByte(i - 1) == '\r') {
                i--;
            }
        } else {
            offset = totalLength;
        }
        return i;
    }
}