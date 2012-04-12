/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
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
 *
 */

package org.graylog2.inputs.gelf;

import org.apache.log4j.Logger;
import org.graylog2.inputs.gelf.processing.GELFProcessor;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

/**
 * GELFUDPDispatcher.java: 12.04.2012 10:40:21
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFUDPDispatcher extends FrameDecoder {

    private static final Logger LOG = Logger.getLogger(GELFUDPDispatcher.class);

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        GELFProcessor processor = new GELFProcessor();
        processor.messageReceived(new GELFMessage(buffer.array()));

        return buffer.readBytes(buffer.readableBytes());
    }

}
