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
package org.graylog2.inputs.transports.netty;

import io.netty.channel.ChannelFactory;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ServerSocketChannelFactory implements ChannelFactory<ServerSocketChannel> {
    private final NettyTransportType transportType;

    public ServerSocketChannelFactory(NettyTransportType transportType) {
        this.transportType = transportType;
    }

    @Override
    public ServerSocketChannel newChannel() {
        switch (transportType) {
            case EPOLL:
                return new EpollServerSocketChannel();
            case KQUEUE:
                return new KQueueServerSocketChannel();
            case NIO:
                return new NioServerSocketChannel();
            default:
                throw new IllegalArgumentException("Invalid or unknown Netty transport type " + transportType);
        }
    }
}
