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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import io.netty.channel.EventLoopGroup;
import org.graylog2.configuration.TLSProtocolsConfiguration;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.inputs.util.ThroughputCounter;

/**
 * Raw version of the HttpTransport which uses the `/raw` path instead of the `/gelf` path.
 */
public class RawHttpTransport extends AbstractHttpTransport {
    private static final String PATH = "/raw";

    @AssistedInject
    public RawHttpTransport(@Assisted Configuration configuration,
                            EventLoopGroup eventLoopGroup,
                            EventLoopGroupFactory eventLoopGroupFactory,
                            NettyTransportConfiguration nettyTransportConfiguration,
                            ThroughputCounter throughputCounter,
                            LocalMetricRegistry localRegistry,
                            TLSProtocolsConfiguration tlsConfiguration) {
        super(configuration, eventLoopGroup, eventLoopGroupFactory, nettyTransportConfiguration,
                throughputCounter, localRegistry, tlsConfiguration, PATH);

    }

    @FactoryClass
    public interface Factory extends Transport.Factory<RawHttpTransport> {
        @Override
        RawHttpTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractHttpTransport.Config {
    }
}
