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

import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.IngestEndpointConfig;
import org.graylog2.plugin.inputs.transports.AbstractTcpTransport;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectorIngestHttpTransportConfigTest {

    @Mock
    CollectorsConfigService collectorsConfigService;

    @Test
    void portDescriptionIncludesConfiguredPort() {
        final var config = CollectorsConfig.builder()
                .http(new IngestEndpointConfig("host", 14401))
                .build();
        when(collectorsConfigService.get()).thenReturn(Optional.of(config));

        final var transportConfig = new CollectorIngestHttpTransport.Config(collectorsConfigService);
        final var portField = transportConfig.getRequestedConfiguration().getField(NettyTransport.CK_PORT);

        assertThat(portField.getDescription()).contains("port 14401");
    }

    @Test
    void portDescriptionFallsBackWhenNoConfig() {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        final var transportConfig = new CollectorIngestHttpTransport.Config(collectorsConfigService);
        final var portField = transportConfig.getRequestedConfiguration().getField(NettyTransport.CK_PORT);

        assertThat(portField.getDescription())
                .contains("collectors settings")
                .doesNotContain("port 14401");
    }

    @Test
    void portFieldHasPortNumberAttribute() {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        final var transportConfig = new CollectorIngestHttpTransport.Config(collectorsConfigService);
        final var portField = transportConfig.getRequestedConfiguration().getField(NettyTransport.CK_PORT);

        assertThat(portField.getAttributes()).contains("is_port_number");
    }

    @Test
    void keepAliveIsEnabledByDefault() {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        final var transportConfig = new CollectorIngestHttpTransport.Config(collectorsConfigService);
        final var keepAliveField = transportConfig.getRequestedConfiguration()
                .getField(AbstractTcpTransport.CK_TCP_KEEPALIVE);

        assertThat(keepAliveField.getDefaultValue()).isEqualTo(true);
    }
}
