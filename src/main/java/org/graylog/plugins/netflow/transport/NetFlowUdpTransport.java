/*
 * Copyright 2017 Graylog Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.graylog.plugins.netflow.transport;

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

/**
 * This UDP transport is largely identical to its superclass, but replaces the codec aggregator and its handler with custom
 * implementations that are able to pass the remote address.
 *
 * Without the remote address the NetFlow V9 code cannot distinguish between flows from different exporters and thus might
 * handle template flows incorrectly should they differ between exporters.
 *
 * @see <a href="https://tools.ietf.org/html/rfc3954#section-5.1">RFC 3953 - Source ID</a>
 */
public class NetFlowUdpTransport extends UdpTransport {
    @Inject
    public NetFlowUdpTransport(@Assisted Configuration configuration,
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
        handlers.replace("codec-aggregator", () -> new NetflowMessageAggregationHandler(aggregator, localRegistry));
        handlers.remove("udp-datagram");

        return handlers;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<NetFlowUdpTransport> {
        @Override
        NetFlowUdpTransport create(Configuration configuration);

        @Override
        NetFlowUdpTransport.Config getConfig();
    }

    @ConfigClass
    public static class Config extends UdpTransport.Config {
    }
}
