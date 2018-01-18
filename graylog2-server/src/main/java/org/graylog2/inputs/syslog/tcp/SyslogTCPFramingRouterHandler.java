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
package org.graylog2.inputs.syslog.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.util.ReferenceCountUtil;

public class SyslogTCPFramingRouterHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final int maxFrameLength;
    private final ByteBuf[] delimiter;
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

            handler.channelRead(ctx, ReferenceCountUtil.retain(msg));
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private boolean usesOctetCountFraming(ByteBuf message) {
        // Octet counting framing needs to start with a non-zero digit.
        // See: http://tools.ietf.org/html/rfc6587#section-3.4.1
        final byte firstByte = message.getByte(0);
        return '0' < firstByte && firstByte <= '9';
    }
}
