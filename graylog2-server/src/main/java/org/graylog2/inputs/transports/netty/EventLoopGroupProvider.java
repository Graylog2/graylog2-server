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
package org.graylog2.inputs.transports.netty;

import com.codahale.metrics.MetricRegistry;
import io.netty.channel.EventLoopGroup;
import org.graylog2.inputs.transports.NettyTransportConfiguration;

import javax.inject.Inject;
import javax.inject.Provider;

public class EventLoopGroupProvider implements Provider<EventLoopGroup> {
    private final EventLoopGroupFactory eventLoopGroupFactory;
    private final NettyTransportConfiguration configuration;
    private final MetricRegistry metricRegistry;

    @Inject
    public EventLoopGroupProvider(EventLoopGroupFactory eventLoopGroupFactory,
                                  NettyTransportConfiguration configuration,
                                  MetricRegistry metricRegistry) {
        this.eventLoopGroupFactory = eventLoopGroupFactory;
        this.configuration = configuration;
        this.metricRegistry = metricRegistry;
    }

    @Override
    public EventLoopGroup get() {
        final String name = "netty-transport";
        final int numThreads = configuration.getNumThreads();
        return eventLoopGroupFactory.create(numThreads, metricRegistry, name);
    }
}
