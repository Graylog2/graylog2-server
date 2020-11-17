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

import io.netty.channel.ChannelFactory;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class DatagramChannelFactory implements ChannelFactory<DatagramChannel> {
    private final NettyTransportType transportType;

    public DatagramChannelFactory(NettyTransportType transportType) {
        this.transportType = transportType;
    }

    @Override
    public DatagramChannel newChannel() {
        switch (transportType) {
            case EPOLL:
                return new EpollDatagramChannel();
            case KQUEUE:
                return new KQueueDatagramChannel();
            case NIO:
                return new NioDatagramChannel();
            default:
                throw new IllegalArgumentException("Invalid or unknown Netty transport type " + transportType);
        }
    }
}
