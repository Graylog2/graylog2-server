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
package org.graylog2.plugin.inputs.transports;

import com.google.common.collect.ImmutableMap;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import org.graylog.testing.RestoreSystemProperties;
import org.graylog2.configuration.TLSProtocolsConfiguration;
import org.graylog2.inputs.transports.NettyTransportConfiguration;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(RestoreSystemProperties.class)
public class AbstractTcpTransportTest {

    @TempDir
    public File temporaryFolder;

    @Mock
    private MessageInput input;

    @Mock
    private TLSProtocolsConfiguration tlsConfiguration;

    private ThroughputCounter throughputCounter;
    private LocalMetricRegistry localRegistry;
    private NioEventLoopGroup eventLoopGroup;
    private EventLoopGroupFactory eventLoopGroupFactory;
    private final NettyTransportConfiguration nettyTransportConfiguration = new NettyTransportConfiguration("nio", "jdk", 2);

    @BeforeEach
    public void setUp() {
        eventLoopGroup = new NioEventLoopGroup();
        eventLoopGroupFactory = new EventLoopGroupFactory(nettyTransportConfiguration);
        throughputCounter = new ThroughputCounter(eventLoopGroup);
        localRegistry = new LocalMetricRegistry();
    }

    @AfterEach
    public void tearDown() {
        eventLoopGroup.shutdownGracefully();
    }

    @Test
    public void getChildChannelHandlersGeneratesSelfSignedCertificates() throws Exception {
        final Configuration configuration = new Configuration(ImmutableMap.of(
            "bind_address", "localhost",
            "port", 12345,
            "tls_enable", true)
        );

        final AbstractTcpTransport transport = new AbstractTcpTransport(
                configuration, throughputCounter, localRegistry, eventLoopGroup, eventLoopGroupFactory, nettyTransportConfiguration, tlsConfiguration) {
        };
        final MessageInput input = mock(MessageInput.class);
        assertThat(transport.getChildChannelHandlers(input)).containsKey("tls");
    }

    @Test
    public void getChildChannelHandlersFailsIfTempDirDoesNotExist() throws Exception {
        final File tmpDir = newFolder(temporaryFolder, "junit");
        assumeTrue(tmpDir.delete());
        System.setProperty("java.io.tmpdir", tmpDir.getAbsolutePath());

        final Configuration configuration = new Configuration(ImmutableMap.of(
            "bind_address", "localhost",
            "port", 12345,
            "tls_enable", true)
        );

        final AbstractTcpTransport transport = new AbstractTcpTransport(
            configuration, throughputCounter, localRegistry, eventLoopGroup, eventLoopGroupFactory, nettyTransportConfiguration, tlsConfiguration) {};

        assertThatThrownBy(() -> transport.getChildChannelHandlers(input))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Couldn't write to temporary directory: " + tmpDir.getAbsolutePath());
    }

    @Test
    public void getChildChannelHandlersFailsIfTempDirIsNotWritable() throws Exception {
        final File tmpDir = newFolder(temporaryFolder, "junit");
        assumeTrue(tmpDir.setWritable(false));
        assumeFalse(tmpDir.canWrite());
        System.setProperty("java.io.tmpdir", tmpDir.getAbsolutePath());

        final Configuration configuration = new Configuration(ImmutableMap.of(
            "bind_address", "localhost",
            "port", 12345,
            "tls_enable", true)
        );

        final AbstractTcpTransport transport = new AbstractTcpTransport(
            configuration, throughputCounter, localRegistry, eventLoopGroup, eventLoopGroupFactory, nettyTransportConfiguration, tlsConfiguration) {};

        Throwable exception = assertThrows(IllegalStateException.class, () ->

            transport.getChildChannelHandlers(input));
        org.hamcrest.MatcherAssert.assertThat(exception.getMessage(), containsString("Couldn't write to temporary directory: " + tmpDir.getAbsolutePath()));
    }

    @Test
    public void getChildChannelHandlersFailsIfTempDirIsNoDirectory() throws Exception {
        final File file = File.createTempFile("junit", null, temporaryFolder);
        assumeTrue(file.isFile());
        System.setProperty("java.io.tmpdir", file.getAbsolutePath());

        final Configuration configuration = new Configuration(ImmutableMap.of(
            "bind_address", "localhost",
            "port", 12345,
            "tls_enable", true)
        );

        final AbstractTcpTransport transport = new AbstractTcpTransport(
            configuration, throughputCounter, localRegistry, eventLoopGroup, eventLoopGroupFactory, nettyTransportConfiguration, tlsConfiguration) {};

        Throwable exception = assertThrows(IllegalStateException.class, () ->

            transport.getChildChannelHandlers(input));
        org.hamcrest.MatcherAssert.assertThat(exception.getMessage(), containsString("Couldn't write to temporary directory: " + file.getAbsolutePath()));
    }

    @Test
    @Disabled("Disabled test due to being unreliable. For details see https://github.com/Graylog2/graylog2-server/issues/4702.")
    public void testTrafficCounter() throws Exception {
        final Configuration configuration = new Configuration(ImmutableMap.of(
                "bind_address", "127.0.0.1",
                "port", 0));
        final AbstractTcpTransport transport = new AbstractTcpTransport(
                configuration, throughputCounter, localRegistry, eventLoopGroup, eventLoopGroupFactory, nettyTransportConfiguration, tlsConfiguration) {
        };
        transport.launch(input);

        await().atMost(5, TimeUnit.SECONDS).until(() -> transport.getLocalAddress() != null);
        final InetSocketAddress localAddress = (InetSocketAddress) transport.getLocalAddress();
        assertThat(localAddress).isNotNull();

        final ChannelFuture channelFuture = clientChannel(localAddress.getHostString(), localAddress.getPort());

        channelFuture.channel()
                .writeAndFlush(Unpooled.copiedBuffer(new byte[1024]))
                .syncUninterruptibly();
        channelFuture.channel()
                .writeAndFlush(Unpooled.copiedBuffer(new byte[1024]))
                .addListener(ChannelFutureListener.CLOSE)
                .syncUninterruptibly();

        // Wait 1s so that the cumulative throughput can be calculated
        Thread.sleep(1000L);

        assertThat(throughputCounter.gauges().get(ThroughputCounter.READ_BYTES_TOTAL).getValue()).isEqualTo(2048L);
        assertThat(throughputCounter.gauges().get(ThroughputCounter.READ_BYTES_1_SEC).getValue()).isEqualTo(2048L);
    }

    @Test
    @Disabled("Disabled test due to being unreliable. For details see https://github.com/Graylog2/graylog2-server/issues/4791.")
    public void testConnectionCounter() throws Exception {
        final Configuration configuration = new Configuration(ImmutableMap.of(
                "bind_address", "127.0.0.1",
                "port", 0));
        final AbstractTcpTransport transport = new AbstractTcpTransport(
                configuration, throughputCounter, localRegistry, eventLoopGroup, eventLoopGroupFactory, nettyTransportConfiguration, tlsConfiguration) {
        };
        transport.launch(input);

        await().atMost(5, TimeUnit.SECONDS).until(() -> transport.getLocalAddress() != null);
        final InetSocketAddress localAddress = (InetSocketAddress) transport.getLocalAddress();
        assertThat(localAddress).isNotNull();

        final ChannelFuture future1 = clientChannel(localAddress.getHostString(), localAddress.getPort()).channel()
                .writeAndFlush(Unpooled.EMPTY_BUFFER)
                .addListener(ChannelFutureListener.CLOSE)
                .syncUninterruptibly();
        final ChannelFuture future2 = clientChannel(localAddress.getHostString(), localAddress.getPort()).channel()
                .writeAndFlush(Unpooled.EMPTY_BUFFER)
                .syncUninterruptibly();

        // TODO: Get rid of this (arbitrary) wait time
        Thread.sleep(100L);

        assertThat(future1.channel().isActive()).isFalse();
        assertThat(future2.channel().isActive()).isTrue();
        assertThat(localRegistry.getGauges().get("open_connections").getValue()).isEqualTo(1);
        assertThat(localRegistry.getGauges().get("total_connections").getValue()).isEqualTo(2L);

        future2.channel().close().syncUninterruptibly();

        // TODO: Get rid of this (arbitrary) wait time
        Thread.sleep(100L);

        assertThat(future1.channel().isActive()).isFalse();
        assertThat(future2.channel().isActive()).isFalse();
        assertThat(localRegistry.getGauges().get("open_connections").getValue()).isEqualTo(0);
        assertThat(localRegistry.getGauges().get("total_connections").getValue()).isEqualTo(2L);
    }

    private ChannelFuture clientChannel(String hostname, int port) {
        return new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler())
                .connect(hostname, port)
                .syncUninterruptibly();
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}
