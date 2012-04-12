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
import org.elasticsearch.common.netty.channel.ExceptionEvent;
import org.graylog2.gelf.GELFUtilities;
import org.graylog2.inputs.gelf.processing.SimpleGELFProcessor;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.compression.ZlibDecoder;
import org.jboss.netty.handler.codec.compression.ZlibWrapper;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

/**
 * GELFUDPDecoder.java: 12.04.2012 10:40:21
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFUDPDecoder extends FrameDecoder {

    private static final Logger LOG = Logger.getLogger(GELFUDPDecoder.class);

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        if (buffer.readableBytes() < GELFUtilities.GELF_HEADER_TYPE_LENGTH) {
            LOG.debug("Skipping GELF packet that is not even long enough to contain the required GELF header.");
            return null;
        }

        final ChannelPipeline pipeline = ctx.getPipeline();

        switch (GELFUtilities.getGELFType(extractMagicBytes(buffer))) {
            case GELFUtilities.TYPE_GZIP:
                LOG.debug("Received GZIP compressed GELF message.");
                pipeline.addLast("gzipdeflater", new ZlibDecoder(ZlibWrapper.GZIP));
                pipeline.addLast("handler", new SimpleGELFProcessor());
                break;
            case GELFUtilities.TYPE_ZLIB:
                LOG.debug("Received ZLIB compressed GELF message.");
                pipeline.addLast("zlibdeflater", new ZlibDecoder(ZlibWrapper.ZLIB));
                pipeline.addLast("handler", new SimpleGELFProcessor());
                break;
        }

        pipeline.remove(this);

        return buffer.readBytes(buffer.readableBytes());
    }

    private byte[] extractMagicBytes(ChannelBuffer buffer) {
        final int m1 = buffer.getUnsignedByte(buffer.readerIndex());
        final int m2 = buffer.getUnsignedByte(buffer.readerIndex() + 1);
        return new byte[] {(byte) m1, (byte) m2};
    }

}
