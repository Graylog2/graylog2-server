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

import org.graylog.security.certutil.InMemoryClusterConfigService;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BeatsTransportConfigTest {
    private final InMemoryClusterConfigService clusterConfigService = new InMemoryClusterConfigService();

    @Test
    public void getRequestedConfigurationOverridesDefaultPort() throws Exception {
        final BeatsTransport.Config config = new BeatsTransport.Config(clusterConfigService);
        final ConfigurationRequest requestedConfiguration = config.getRequestedConfiguration();

        assertThat(requestedConfiguration.containsField(NettyTransport.CK_PORT)).isTrue();
        assertThat(requestedConfiguration.getField(NettyTransport.CK_PORT).getDefaultValue()).isEqualTo(5044);
    }

    @Test
    public void maxEventSizeDefaultsToUsingClusterDefault() {
        final ConfigurationRequest requestedConfiguration = new BeatsTransport.Config(clusterConfigService).getRequestedConfiguration();

        final ConfigurationField field = requestedConfiguration.getField(BeatsTransport.CK_MAX_EVENT_SIZE);
        assertThat(field.isOptional()).isEqualTo(ConfigurationField.Optional.OPTIONAL);
        assertThat(field.getDefaultValue()).isEqualTo(0);
        assertThat(field.getDescription()).contains("currently " + BeatsInputDefaultsConfig.DEFAULT_MAX_EVENT_SIZE + " bytes");
    }

    @Test
    public void maxEventSizeDescriptionShowsCurrentClusterDefault() {
        clusterConfigService.write(BeatsInputDefaultsConfig.create(32 * 1024 * 1024));

        final ConfigurationRequest requestedConfiguration = new BeatsTransport.Config(clusterConfigService).getRequestedConfiguration();

        final ConfigurationField field = requestedConfiguration.getField(BeatsTransport.CK_MAX_EVENT_SIZE);
        assertThat(field.getDefaultValue()).isEqualTo(0);
        assertThat(field.getDescription()).contains("currently " + (32 * 1024 * 1024) + " bytes");
    }
}
