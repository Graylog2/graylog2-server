/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.syslog.tcp;

import com.google.common.base.Charsets;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

/**
 * Implements a Netty {@link FrameDecoder} for the Syslog octet counting framing. (RFC6587)
 *
 * @see <a href="http://tools.ietf.org/html/rfc6587#section-3.4.1">RFC6587 Octet Counting</a>
 */
public class SyslogOctetCountFrameDecoder extends FrameDecoder {
    @Override
    protected Object decode(final ChannelHandlerContext ctx,
                            final Channel channel,
                            final ChannelBuffer buffer) throws Exception {
        final int frameSizeValueLength = findFrameSizeValueLength(buffer);

        // We have not found the frame length value byte size yet.
        if (frameSizeValueLength <= 0) {
            return null;
        }

        // Convert the frame length value bytes into an integer without mutating the buffer reader index.
        final String lengthString = buffer.slice(buffer.readerIndex(), frameSizeValueLength).toString(Charsets.UTF_8);
        final int length = Integer.parseInt(lengthString);

        if (buffer.readableBytes() < length) {
            // We cannot read the complete frame yet.
            return null;
        } else {
            // Skip the frame length value bytes and the whitespace that follows it.
            buffer.skipBytes(frameSizeValueLength + 1);
        }

        final ChannelBuffer frame = extractFrame(buffer, buffer.readerIndex(), length);

        // Advance the reader index because extractFrame() does not do that.
        buffer.skipBytes(length);

        return frame;
    }

    /**
     * Find the byte length of the frame length value.
     *
     * @param buffer The channel buffer
     * @return The length of the frame length value
     */
    private int findFrameSizeValueLength(final ChannelBuffer buffer) {
        final int n = buffer.writerIndex();

        for (int i = buffer.readerIndex(); i < n; i ++) {
            final byte b = buffer.getByte(i);

            if (b == ' ') {
                return i - buffer.readerIndex();
            }
        }

        return -1;  // Not found.
    }
}
