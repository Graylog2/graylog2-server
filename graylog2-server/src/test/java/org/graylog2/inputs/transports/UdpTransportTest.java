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
package org.graylog2.inputs.transports;

import com.codahale.metrics.Gauge;
import com.github.joschi.jadconfig.util.Size;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang3.SystemUtils;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.graylog2.shared.SuppressForbidden;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UdpTransportTest {
    private static final String BIND_ADDRESS = "127.0.0.1";
    private static final int PORT = 0;
    private static final int RECV_BUFFER_SIZE = 1024;
    private static final ImmutableMap<String, Object> CONFIG_SOURCE = ImmutableMap.of(
            NettyTransport.CK_BIND_ADDRESS, BIND_ADDRESS,
            NettyTransport.CK_PORT, PORT,
            NettyTransport.CK_RECV_BUFFER_SIZE, RECV_BUFFER_SIZE,
            NettyTransport.CK_NUMBER_WORKER_THREADS, 1);
    private static final Configuration CONFIGURATION = new Configuration(CONFIG_SOURCE);

    private final NettyTransportConfiguration nettyTransportConfiguration = new NettyTransportConfiguration("nio", "jdk", 1);
    private UdpTransport udpTransport;
    private EventLoopGroup eventLoopGroup;
    private EventLoopGroupFactory eventLoopGroupFactory;
    private ThroughputCounter throughputCounter;
    private LocalMetricRegistry localMetricRegistry;

    @Before
    @SuppressForbidden("Executors#newSingleThreadExecutor() is okay for tests")
    public void setUp() throws Exception {
        eventLoopGroupFactory = new EventLoopGroupFactory(nettyTransportConfiguration);
        localMetricRegistry = new LocalMetricRegistry();
        eventLoopGroup = eventLoopGroupFactory.create(1, localMetricRegistry,"test");
        throughputCounter = new ThroughputCounter(eventLoopGroup);
        udpTransport = new UdpTransport(CONFIGURATION, eventLoopGroupFactory, nettyTransportConfiguration, throughputCounter, localMetricRegistry);
    }

    @After
    public void tearDown() {
        eventLoopGroup.shutdownGracefully();
    }

    @Test
    public void transportReceivesDataSmallerThanRecvBufferSize() throws Exception {
        final CountingChannelUpstreamHandler handler = new CountingChannelUpstreamHandler();
        final UdpTransport transport = launchTransportForBootStrapTest(handler);
        await().atMost(5, TimeUnit.SECONDS).until(() -> transport.getLocalAddress() != null);
        final InetSocketAddress localAddress = (InetSocketAddress) transport.getLocalAddress();

        sendUdpDatagram(BIND_ADDRESS, localAddress.getPort(), 100);
        await().atMost(5, TimeUnit.SECONDS).until(() -> !handler.getBytesWritten().isEmpty());
        transport.stop();

        assertThat(handler.getBytesWritten()).containsOnly(100);
    }

    @Test
    public void transportReceivesDataExactlyRecvBufferSize() throws Exception {
        final CountingChannelUpstreamHandler handler = new CountingChannelUpstreamHandler();
        final UdpTransport transport = launchTransportForBootStrapTest(handler);
        await().atMost(5, TimeUnit.SECONDS).until(() -> transport.getLocalAddress() != null);
        final InetSocketAddress localAddress = (InetSocketAddress) transport.getLocalAddress();

        // This will be variable depending on the version of the IP protocol and the UDP packet size.
        final int udpOverhead = 16;
        final int maxPacketSize = RECV_BUFFER_SIZE - udpOverhead;

        sendUdpDatagram(BIND_ADDRESS, localAddress.getPort(), maxPacketSize);
        await().atMost(5, TimeUnit.SECONDS).until(() -> !handler.getBytesWritten().isEmpty());
        transport.stop();

        assertThat(handler.getBytesWritten()).containsOnly(maxPacketSize);
    }

    @Test
    public void transportDiscardsDataLargerRecvBufferSizeOnMacOsX() throws Exception {
        assumeTrue("Skipping test intended for MacOS X systems", SystemUtils.IS_OS_MAC_OSX);

        final CountingChannelUpstreamHandler handler = new CountingChannelUpstreamHandler();
        final UdpTransport transport = launchTransportForBootStrapTest(handler);
        await().atMost(5, TimeUnit.SECONDS).until(() -> transport.getLocalAddress() != null);
        final InetSocketAddress localAddress = (InetSocketAddress) transport.getLocalAddress();

        sendUdpDatagram(BIND_ADDRESS, localAddress.getPort(), 2 * RECV_BUFFER_SIZE);
        Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);

        transport.stop();

        assertThat(handler.getBytesWritten()).isEmpty();
    }

    @Test
    public void transportTruncatesDataLargerRecvBufferSizeOnLinux() throws Exception {
        assumeTrue("Skipping test intended for Linux systems", SystemUtils.IS_OS_LINUX);

        final CountingChannelUpstreamHandler handler = new CountingChannelUpstreamHandler();
        final UdpTransport transport = launchTransportForBootStrapTest(handler);
        await().atMost(5, TimeUnit.SECONDS).until(() -> transport.getLocalAddress() != null);
        final InetSocketAddress localAddress = (InetSocketAddress) transport.getLocalAddress();

        sendUdpDatagram(BIND_ADDRESS, localAddress.getPort(), 2 * RECV_BUFFER_SIZE);
        await().atMost(5, TimeUnit.SECONDS).until(() -> !handler.getBytesWritten().isEmpty());
        transport.stop();

        assertThat(handler.getBytesWritten()).containsExactly(RECV_BUFFER_SIZE);
    }

    @Test
    public void receiveBufferSizeIsDefaultSize() {
        assertThat(udpTransport.getBootstrap(mock(MessageInput.class)).config().options().get(ChannelOption.SO_RCVBUF)).isEqualTo(RECV_BUFFER_SIZE);
    }

    @Test
    public void receiveBufferSizeIsNotLimited() {
        final int recvBufferSize = Ints.saturatedCast(Size.megabytes(1L).toBytes());
        ImmutableMap<String, Object> source = ImmutableMap.of(
                NettyTransport.CK_BIND_ADDRESS, BIND_ADDRESS,
                NettyTransport.CK_PORT, PORT,
                NettyTransport.CK_RECV_BUFFER_SIZE, recvBufferSize);
        Configuration config = new Configuration(source);
        UdpTransport udpTransport = new UdpTransport(config, eventLoopGroupFactory, nettyTransportConfiguration, throughputCounter, new LocalMetricRegistry());

        assertThat(udpTransport.getBootstrap(mock(MessageInput.class)).config().options().get(ChannelOption.SO_RCVBUF)).isEqualTo(recvBufferSize);
    }

    @Test
    public void receiveBufferSizePredictorIsUsingDefaultSize() {
        FixedRecvByteBufAllocator recvByteBufAllocator =
                (FixedRecvByteBufAllocator) udpTransport.getBootstrap(mock(MessageInput.class)).config().options().get(ChannelOption.RCVBUF_ALLOCATOR);
        assertThat(recvByteBufAllocator.newHandle().guess()).isEqualTo(RECV_BUFFER_SIZE);
    }

    @Test
    public void getMetricSetReturnsLocalMetricRegistry() {
        assertThat(udpTransport.getMetricSet()).isSameAs(localMetricRegistry);
    }

    @Test
    public void testDefaultReceiveBufferSize() {
        final UdpTransport.Config config = new UdpTransport.Config();
        final ConfigurationRequest requestedConfiguration = config.getRequestedConfiguration();

        assertThat(requestedConfiguration.getField(NettyTransport.CK_RECV_BUFFER_SIZE).getDefaultValue()).isEqualTo(262144);
    }

    @Test
    public void testTrafficCounter() throws Exception {
        final CountingChannelUpstreamHandler handler = new CountingChannelUpstreamHandler();
        final UdpTransport transport = launchTransportForBootStrapTest(handler);
        try {
            await().atMost(5, TimeUnit.SECONDS).until(() -> transport.getLocalAddress() != null);
            final InetSocketAddress localAddress = (InetSocketAddress) transport.getLocalAddress();

            sendUdpDatagram(BIND_ADDRESS, localAddress.getPort(), 512);
            await().atMost(5, TimeUnit.SECONDS).until(() -> handler.getBytesWritten().size() == 1);
            assertThat(handler.getBytesWritten()).containsExactly(512);

            sendUdpDatagram(BIND_ADDRESS, localAddress.getPort(), 512);
            await().atMost(5, TimeUnit.SECONDS).until(() -> handler.getBytesWritten().size() == 2);
            assertThat(handler.getBytesWritten()).containsExactly(512, 512);

        } finally {
            transport.stop();
        }

        final Map<String, Gauge<Long>> gauges = throughputCounter.gauges();
        assertThat(gauges.get(ThroughputCounter.READ_BYTES_TOTAL).getValue()).isEqualTo(1024L);
    }

    private UdpTransport launchTransportForBootStrapTest(final ChannelInboundHandler channelHandler) throws MisfireException {
        final UdpTransport transport = new UdpTransport(CONFIGURATION, eventLoopGroupFactory, nettyTransportConfiguration, throughputCounter, new LocalMetricRegistry()) {
            @Override
            protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getChannelHandlers(MessageInput input) {
                final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlers = new LinkedHashMap<>();
                handlers.put("logging", () -> new LoggingHandler(LogLevel.INFO));
                handlers.put("counter", () -> channelHandler);
                handlers.putAll(super.getChannelHandlers(input));
                return handlers;
            }
        };

        final MessageInput messageInput = mock(MessageInput.class);
        when(messageInput.getId()).thenReturn("TEST");
        when(messageInput.getName()).thenReturn("TEST");

        transport.launch(messageInput);

        return transport;
    }

    private void sendUdpDatagram(String hostname, int port, int size) throws IOException {
        final InetAddress address = InetAddress.getByName(hostname);
        final byte[] data = new byte[size];
        final DatagramPacket packet = new DatagramPacket(data, data.length, address, port);

        try (DatagramSocket toSocket = new DatagramSocket()) {
            toSocket.send(packet);
        }
    }

    public static class CountingChannelUpstreamHandler extends SimpleChannelInboundHandler< io.netty.channel.socket.DatagramPacket> {
        private final List<Integer> bytesWritten = new CopyOnWriteArrayList<>();

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, io.netty.channel.socket.DatagramPacket msg) throws Exception {
                bytesWritten.add(msg.content().readableBytes());
                ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
        }

        List<Integer> getBytesWritten() {
            return bytesWritten;
        }
    }
}