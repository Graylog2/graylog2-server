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
