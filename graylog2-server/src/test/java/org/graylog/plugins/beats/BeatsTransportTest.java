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
package org.graylog.plugins.beats;

import io.netty.channel.nio.NioEventLoopGroup;
import org.graylog.security.certutil.InMemoryClusterConfigService;
import org.graylog2.configuration.TLSProtocolsConfiguration;
import org.graylog2.inputs.transports.NettyTransportConfiguration;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class BeatsTransportTest {

    private NioEventLoopGroup eventLoopGroup;
    private InMemoryClusterConfigService clusterConfigService;

    @Mock
    private TLSProtocolsConfiguration tlsConfiguration;

    @BeforeEach
    public void setUp() {
        eventLoopGroup = new NioEventLoopGroup(1);
        clusterConfigService = new InMemoryClusterConfigService();
    }

    @AfterEach
    public void tearDown() {
        eventLoopGroup.shutdownGracefully();
    }

    @Test
    public void customChildChannelHandlersContainBeatsHandler() {
        final BeatsTransport transport = createTransport(Configuration.EMPTY_CONFIGURATION);

        final MessageInput input = mock(MessageInput.class);
        assertThat(transport.getCustomChildChannelHandlers(input)).containsKey("beats");
    }

    @Test
    public void maxEventSizeFallsBackToBuiltInDefault() {
        final BeatsTransport transport = createTransport(Configuration.EMPTY_CONFIGURATION);

        assertThat(transport.maxEventSize()).isEqualTo(BeatsInputDefaultsConfig.DEFAULT_MAX_EVENT_SIZE);
    }

    @Test
    public void maxEventSizeFallsBackToClusterDefault() {
        clusterConfigService.write(BeatsInputDefaultsConfig.create(32 * 1024 * 1024));
        final BeatsTransport transport = createTransport(Configuration.EMPTY_CONFIGURATION);

        assertThat(transport.maxEventSize()).isEqualTo(32 * 1024 * 1024);
    }

    @Test
    public void maxEventSizeUsesInputSettingOverClusterDefault() {
        clusterConfigService.write(BeatsInputDefaultsConfig.create(32 * 1024 * 1024));
        final BeatsTransport transport = createTransport(new Configuration(Map.of(BeatsTransport.CK_MAX_EVENT_SIZE, 1024)));

        assertThat(transport.maxEventSize()).isEqualTo(1024);
    }

    @Test
    public void maxEventSizeOfZeroUsesClusterDefault() {
        clusterConfigService.write(BeatsInputDefaultsConfig.create(32 * 1024 * 1024));
        final BeatsTransport transport = createTransport(new Configuration(Map.of(BeatsTransport.CK_MAX_EVENT_SIZE, 0)));

        assertThat(transport.maxEventSize()).isEqualTo(32 * 1024 * 1024);
    }

    private BeatsTransport createTransport(Configuration configuration) {
        final NettyTransportConfiguration nettyTransportConfiguration = new NettyTransportConfiguration("nio", "jdk", 1);
        final EventLoopGroupFactory eventLoopGroupFactory = new EventLoopGroupFactory(nettyTransportConfiguration);
        return new BeatsTransport(
                configuration,
                eventLoopGroup,
                eventLoopGroupFactory,
                nettyTransportConfiguration,
                new ThroughputCounter(eventLoopGroup),
                new LocalMetricRegistry(),
                tlsConfiguration,
                mock(EncryptedValueService.class),
                clusterConfigService
        );
    }
}
