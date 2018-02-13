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

import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.graylog.plugins.netflow.codecs.NetflowV9CodecAggregator;
import org.graylog2.inputs.transports.NettyTransportConfiguration;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class NetFlowUdpTransportTest {
    private EventLoopGroup eventLoopGroup;
    private EventLoopGroupFactory eventLoopGroupFactory;
    private NetFlowUdpTransport transport;

    @Before
    public void setUp() {
        final NettyTransportConfiguration nettyTransportConfiguration = new NettyTransportConfiguration("nio", "jdk", 1);
        eventLoopGroupFactory = new EventLoopGroupFactory(nettyTransportConfiguration);
        eventLoopGroup = new NioEventLoopGroup(1);
        transport = new NetFlowUdpTransport(
                Configuration.EMPTY_CONFIGURATION,
                eventLoopGroupFactory,
                nettyTransportConfiguration,
                new ThroughputCounter(eventLoopGroup),
                new LocalMetricRegistry());
        transport.setMessageAggregator(new NetflowV9CodecAggregator());
    }

    @After
    public void tearDown() {
        eventLoopGroup.shutdownGracefully();
    }

    @Test
    public void getChildChannelHandlersContainsCustomCodecAggregator() throws Exception {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlers = transport.getChannelHandlers(mock(MessageInput.class));
        assertThat(handlers)
                .containsKey("codec-aggregator")
                .doesNotContainKey("udp-datagram");

        final ChannelHandler channelHandler = handlers.get("codec-aggregator").call();
        assertThat(channelHandler).isInstanceOf(NetflowMessageAggregationHandler.class);
    }
}