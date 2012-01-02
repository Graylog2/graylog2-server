/**
 * Copyright 2012 Kay Roepke <kroepke@classdump.org>
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

package org.graylog2.messagehandlers.gelf;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class ChunkedGELFHandler extends SimpleChannelUpstreamHandler {
    private static final Logger LOG = Logger.getLogger(ChunkedGELFHandler.class);
    
    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e)
            throws Exception {
        final ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
        final byte[] data = new byte[buffer.readableBytes()];
        buffer.getBytes(0, data);
        LOG.info("received chunked gelf messages, passing on to handler");
        new ChunkedGELFClientHandler(data).handle();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e)
            throws Exception {
        LOG.error("Could not handle chunked GELF message", e.getCause());
        e.getChannel().close();
    }

    
}
