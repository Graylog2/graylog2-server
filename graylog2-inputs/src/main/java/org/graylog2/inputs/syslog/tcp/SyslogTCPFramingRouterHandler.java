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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;

public class SyslogTCPFramingRouterHandler extends SimpleChannelUpstreamHandler {
    private final int maxFrameLength;
    private final ChannelBuffer[] delimiter;
    private boolean routed = false;

    public SyslogTCPFramingRouterHandler(int maxFrameLength, ChannelBuffer[] delimiter) {
        this.maxFrameLength = maxFrameLength;
        this.delimiter = delimiter;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        final ChannelBuffer message = (ChannelBuffer) e.getMessage();

        if (! message.readable()) {
            return;
        }

        if (! routed) {
            if (usesOctetCountFraming(message)) {
                ctx.getPipeline().addAfter(ctx.getName(), "framer-octet", new SyslogOctetCountFrameDecoder());
            } else {
                ctx.getPipeline().addAfter(ctx.getName(), "framer-delimiter", new DelimiterBasedFrameDecoder(maxFrameLength, delimiter));
            }

            routed = true;
        }

        ctx.sendUpstream(e);
    }

    private boolean usesOctetCountFraming(ChannelBuffer message) {
        // Octet counting framing needs to start with a non-zero digit.
        // See: http://tools.ietf.org/html/rfc6587#section-3.4.1
        return '0' < message.getByte(0) && message.getByte(0) <= '9';
    }
}
