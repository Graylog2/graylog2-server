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
package org.graylog2.inputs.transports;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import org.graylog2.inputs.syslog.tcp.SyslogTCPFramingRouterHandler;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.inputs.util.ThroughputCounter;

import java.util.LinkedHashMap;
import java.util.concurrent.Callable;

public class SyslogTcpTransport extends TcpTransport {
    @AssistedInject
    public SyslogTcpTransport(@Assisted Configuration configuration,
                              EventLoopGroup eventLoopGroup,
                              EventLoopGroupFactory eventLoopGroupFactory,
                              NettyTransportConfiguration nettyTransportConfiguration,
                              ThroughputCounter throughputCounter,
                              LocalMetricRegistry localRegistry,
                              org.graylog2.Configuration graylogConfiguration) {
        super(configuration,
                eventLoopGroup,
                eventLoopGroupFactory,
                nettyTransportConfiguration,
                throughputCounter,
                localRegistry,
                graylogConfiguration);
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getCustomChildChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> finalChannelHandlers = new LinkedHashMap<>(super.getCustomChildChannelHandlers(input));

        // Replace the "framer" channel handler inserted by the parent.
        finalChannelHandlers.replace("framer", () -> new SyslogTCPFramingRouterHandler(maxFrameLength, delimiter));

        return finalChannelHandlers;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<SyslogTcpTransport> {
        @Override
        SyslogTcpTransport create(Configuration configuration);
    }

    @ConfigClass
    public static class Config extends TcpTransport.Config {
    }
}
