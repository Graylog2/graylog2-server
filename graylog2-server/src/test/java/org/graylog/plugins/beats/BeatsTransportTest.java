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
package org.graylog.plugins.beats;

import io.netty.channel.nio.NioEventLoopGroup;
import org.graylog2.inputs.transports.NettyTransportConfiguration;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class BeatsTransportTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private NioEventLoopGroup eventLoopGroup;

    @Mock
    private org.graylog2.Configuration graylogConfiguration;

    @Before
    public void setUp() {
        eventLoopGroup = new NioEventLoopGroup(1);
    }

    @After
    public void tearDown() {
        eventLoopGroup.shutdownGracefully();
    }

    @Test
    public void customChildChannelHandlersContainBeatsHandler() {
        final NettyTransportConfiguration nettyTransportConfiguration = new NettyTransportConfiguration("nio", "jdk", 1);
        final EventLoopGroupFactory eventLoopGroupFactory = new EventLoopGroupFactory(nettyTransportConfiguration);
        final BeatsTransport transport = new BeatsTransport(
                Configuration.EMPTY_CONFIGURATION,
                eventLoopGroup,
                eventLoopGroupFactory,
                nettyTransportConfiguration,
                new ThroughputCounter(eventLoopGroup),
                new LocalMetricRegistry(),
                graylogConfiguration
        );

        final MessageInput input = mock(MessageInput.class);
        assertThat(transport.getCustomChildChannelHandlers(input)).containsKey("beats");
    }
}
