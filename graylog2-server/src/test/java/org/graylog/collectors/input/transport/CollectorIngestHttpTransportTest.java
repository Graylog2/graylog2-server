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
package org.graylog.collectors.input.transport;

import io.netty.channel.EventLoopGroup;
import io.netty.handler.timeout.IdleStateHandler;
import org.graylog.collectors.CollectorTLSUtils;
import org.graylog2.configuration.TLSProtocolsConfiguration;
import org.graylog2.inputs.transports.AbstractHttpTransport;
import org.graylog2.inputs.transports.NettyTransportConfiguration;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectorIngestHttpTransportTest {

    @Mock
    private EventLoopGroup eventLoopGroup;
    @Mock
    private EventLoopGroupFactory eventLoopGroupFactory;
    @Mock
    private NettyTransportConfiguration nettyTransportConfiguration;
    @Mock
    private ThroughputCounter throughputCounter;
    @Mock
    private TLSProtocolsConfiguration tlsConfiguration;
    @Mock
    private CollectorTLSUtils tlsUtils;
    @Mock
    private EncryptedValueService encryptedValueService;
    @Mock
    private CollectorIngestHttpHandler.Factory httpHandlerFactory;
    @Mock
    private MessageInput input;

    /**
     * The idle connection timeout is part of the transport's forced, non-configurable envelope (like
     * mTLS): connections must be closed well before their fingerprint cache entry can idle out, so that
     * per-request binding lookups are always cache hits (see {@code CollectorFingerprintCache}). A user
     * config of {@code 0} would disable the read-timeout handler entirely and void that guarantee, so it
     * must be overridden.
     */
    @Test
    void idleConnectionTimeoutIsForcedEvenWhenConfiguredOff() throws Exception {
        final var transport = buildTransport(Map.of(AbstractHttpTransport.CK_IDLE_WRITER_TIMEOUT, 0));

        final var handlers = transport.getCustomChildChannelHandlers(input);

        assertThat(handlers).containsKey("read-timeout-handler");
        final var handler = (IdleStateHandler) handlers.get("read-timeout-handler").call();
        assertThat(handler.getReaderIdleTimeInMillis()).isEqualTo(Duration.ofSeconds(60).toMillis());
    }

    private CollectorIngestHttpTransport buildTransport(Map<String, Object> userConfig) {
        when(throughputCounter.gauges()).thenReturn(Map.of());
        return new CollectorIngestHttpTransport(new Configuration(userConfig), eventLoopGroup,
                eventLoopGroupFactory, nettyTransportConfiguration, throughputCounter,
                new LocalMetricRegistry(), tlsConfiguration, Set.of(), tlsUtils, encryptedValueService,
                httpHandlerFactory);
    }
}
