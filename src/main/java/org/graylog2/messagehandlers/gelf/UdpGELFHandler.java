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
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.compression.ZlibDecoder;
import org.jboss.netty.handler.codec.compression.ZlibWrapper;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

public class UdpGELFHandler extends FrameDecoder {
    private static final Logger LOG = Logger.getLogger(UdpGELFHandler.class);
    
    @Override
    protected Object decode(final ChannelHandlerContext ctx, final Channel channel,
            final ChannelBuffer buffer) throws Exception {
        if (buffer.readableBytes() < 2) {
            return null;
        }
        final int magic1 = buffer.getUnsignedByte(buffer.readerIndex());
        final int magic2 = buffer.getUnsignedByte(buffer.readerIndex() + 1);
        final byte[] data = new byte[] {(byte) magic1, (byte) magic2};
        
        switch (GELF.getGELFType(data)) {
        case GELF.TYPE_GZIP:
            enableGzip(ctx);
            break;
        case GELF.TYPE_ZLIB:
            enableZlib(ctx);
            break;
        case GELF.TYPE_CHUNKED:
            enableChunked(ctx);
            break;
        default: {
            // Unknown protocol; discard everything and close the connection.
            buffer.skipBytes(buffer.readableBytes());
            ctx.getChannel().close();
            return null;
        }
        }
        LOG.info("passing on bytes to next handler");
        return buffer.readBytes(buffer.readableBytes());
    }

    private void enableChunked(final ChannelHandlerContext ctx) {
        LOG.info("enabling handling of chunked gelf messages on this connection");
        final ChannelPipeline pipeline = ctx.getPipeline();
        pipeline.addLast("chunked", new ChunkedGELFHandler());
        pipeline.remove(this);
    }

    private void enableZlib(final ChannelHandlerContext ctx) {
        LOG.info("enabling handling of zlib gelf messages on this connection");

        final ChannelPipeline pipeline = ctx.getPipeline();
        pipeline.addLast("zlibdeflater", new ZlibDecoder(ZlibWrapper.ZLIB));
        pipeline.addLast("handler", new SimpleGELFHandler());
        pipeline.remove(this);
    }

    private void enableGzip(final ChannelHandlerContext ctx) {
        LOG.info("enabling handling of gzip gelf messages on this connection");

        final ChannelPipeline pipeline = ctx.getPipeline();
        pipeline.addLast("gzipdeflater", new ZlibDecoder(ZlibWrapper.GZIP));
        pipeline.addLast("handler", new SimpleGELFHandler());
        pipeline.remove(this);
    }
}
