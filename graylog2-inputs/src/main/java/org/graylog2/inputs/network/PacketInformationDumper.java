/*
 * Copyright 2014 TORCH GmbH
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
 */
package org.graylog2.inputs.network;

import org.graylog2.plugin.inputs.MessageInput;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketInformationDumper extends SimpleChannelUpstreamHandler {
    private static final Logger classLogger = LoggerFactory.getLogger(PacketInformationDumper.class);
    private final Logger log;
    private final MessageInput sourceInput;

    public PacketInformationDumper(MessageInput sourceInput) {
        this.sourceInput = sourceInput;
        log = LoggerFactory.getLogger(PacketInformationDumper.class.getCanonicalName() + "." + sourceInput.getId());
        classLogger.debug("Set {} to TRACE for network packet metadata dumps of input {}", log.getName(), sourceInput.getUniqueReadableId());
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        try {
            if (log.isTraceEnabled()) {
                final ChannelBuffer message = (ChannelBuffer) e.getMessage();
                log.trace("Recv network data: {} bytes via input '{}' <{}> from remote address {}",
                          new Object[] {message.readableBytes(), sourceInput.getName(), sourceInput.getId(), e.getRemoteAddress() });
            }
        } finally {
            super.messageReceived(ctx, e);
        }
    }
}
