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
package org.graylog2.inputs.transports.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;

public class ExceptionLoggingChannelHandler extends ChannelInboundHandlerAdapter {
    private final MessageInput input;
    private final Logger logger;
    private final boolean keepAliveEnabled;

    public ExceptionLoggingChannelHandler(MessageInput input, Logger logger) {
        this(input, logger, false);
    }

    public ExceptionLoggingChannelHandler(MessageInput input, Logger logger, boolean keepAliveEnabled) {
        this.input = input;
        this.logger = logger;
        this.keepAliveEnabled = keepAliveEnabled;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (logger.isTraceEnabled() && "Connection reset by peer".equals(cause.getMessage())) {
            logger.trace("{} in Input [{}/{}] (channel {})",
                    cause.getMessage(),
                    input.getName(),
                    input.getId(),
                    ctx.channel());
        } else if (this.keepAliveEnabled && cause instanceof ReadTimeoutException) {
            if (logger.isTraceEnabled()) {
                logger.trace("KeepAlive Timeout in input [{}/{}] (channel {})",
                        input.getName(),
                        input.getId(),
                        ctx.channel());
            }
        } else {
            logger.error("Error in Input [{}/{}] (channel {}) (cause {})",
                    input.getName(),
                    input.getId(),
                    ctx.channel(),
                    cause);
        }

        ctx.close();
    }
}
