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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@ChannelHandler.Sharable
public class ConnectionCounter extends ChannelInboundHandlerAdapter {
    private final AtomicInteger connections;
    private final AtomicLong totalConnections;

    public ConnectionCounter(AtomicInteger connections, AtomicLong totalConnections) {
        this.connections = connections;
        this.totalConnections = totalConnections;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        connections.incrementAndGet();
        totalConnections.incrementAndGet();
        ctx.channel().closeFuture().addListener(f -> connections.decrementAndGet());

        super.channelActive(ctx);
    }

    public int getConnectionCount() {
        return connections.get();
    }

    public long getTotalConnections() {
        return totalConnections.get();
    }
}
