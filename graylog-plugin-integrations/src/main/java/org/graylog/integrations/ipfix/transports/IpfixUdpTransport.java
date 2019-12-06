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
package org.graylog.integrations.ipfix.transports;

import com.google.inject.assistedinject.Assisted;
import io.netty.channel.ChannelHandler;
import org.graylog.plugins.netflow.codecs.RemoteAddressCodecAggregator;
import org.graylog2.inputs.transports.NettyTransportConfiguration;
import org.graylog2.inputs.transports.UdpTransport;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.inputs.util.ThroughputCounter;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;

public class IpfixUdpTransport extends UdpTransport {
    @Inject
    public IpfixUdpTransport(@Assisted Configuration configuration,
                             EventLoopGroupFactory eventLoopGroupFactory,
                             NettyTransportConfiguration nettyTransportConfiguration,
                             ThroughputCounter throughputCounter,
                             LocalMetricRegistry localRegistry) {
        super(configuration, eventLoopGroupFactory, nettyTransportConfiguration, throughputCounter, localRegistry);
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlers = new LinkedHashMap<>(super.getChannelHandlers(input));

        // Replace the default "codec-aggregator" handler with one that passes the remote address
        final RemoteAddressCodecAggregator aggregator = (RemoteAddressCodecAggregator) getAggregator();
        handlers.replace("codec-aggregator", () -> new IpfixMessageAggregationHandler(aggregator, localRegistry));
        handlers.remove("udp-datagram");

        return handlers;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<IpfixUdpTransport> {
        @Override
        IpfixUdpTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends UdpTransport.Config {
    }
}
