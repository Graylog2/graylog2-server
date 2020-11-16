/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
