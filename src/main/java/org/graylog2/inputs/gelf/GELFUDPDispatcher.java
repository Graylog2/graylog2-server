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
import org.graylog2.GraylogServer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

/**
 * GELFUDPDispatcher.java: 12.04.2012 10:40:21
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFUDPDispatcher extends FrameDecoder {

    private static final Logger LOG = Logger.getLogger(GELFUDPDispatcher.class);

    private GELFProcessor processor;
    private GraylogServer server;

    public GELFUDPDispatcher(GraylogServer server) {
        this.server = server;
        this.processor = new GELFProcessor(server);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {

        byte[] readable = new byte[buffer.readableBytes()];
        buffer.toByteBuffer().get(readable, buffer.readerIndex(), buffer.readableBytes()); // I'm 12 years old and what is this? There must be a better way.

        GELFMessage msg = new GELFMessage(readable);

        if (msg.getGELFType() == GELFMessage.TYPE_CHUNKED) {
            // This is a GELF message chunk. Add chunk to manager.
            server.getGELFChunkManager().insert(msg.asChunk()); // XXX IMPROVE: this msg.asChunk() is a bit bumpy. better have a chunk from the beginning on.
        } else {
            // This is a non-chunked/complete GELF message. Process it.
            processor.messageReceived(msg);
        }

        return buffer.readBytes(buffer.readableBytes());
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        LOG.warn("Could not handle GELF message.", e.getCause());
    }

}
