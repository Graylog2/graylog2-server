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
package org.graylog2.plugin.inputs.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketInformationDumper extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger LOG = LoggerFactory.getLogger(PacketInformationDumper.class);
    private final Logger sourceInputLog;

    private final String sourceInputName;
    private final String sourceInputId;

    public PacketInformationDumper(MessageInput sourceInput) {
        sourceInputName = sourceInput.getName();
        sourceInputId = sourceInput.getId();
        sourceInputLog = LoggerFactory.getLogger(PacketInformationDumper.class.getCanonicalName() + "." + sourceInputId);
        LOG.debug("Set {} to TRACE for network packet metadata dumps of input {}", sourceInputLog.getName(),
                sourceInput.getUniqueReadableId());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (sourceInputLog.isTraceEnabled()) {
            sourceInputLog.trace("Recv network data: {} bytes via input '{}' <{}> from remote address {}",
                    msg.readableBytes(), sourceInputName, sourceInputId, ctx.channel().remoteAddress());
        }
    }
}
