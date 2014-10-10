/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.transports;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.plugin.collections.Pair;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput2;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.inputs.util.ConnectionCounter;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import javax.inject.Provider;
import java.util.List;
import java.util.concurrent.Executor;

import static com.codahale.metrics.MetricRegistry.name;

public abstract class AbstractTcpTransport extends NettyTransport {
    protected final Executor bossExecutor;
    protected final Executor workerExecutor;
    protected final ConnectionCounter connectionCounter;

    public AbstractTcpTransport(
            Configuration configuration,
            ThroughputCounter throughputCounter,
            MetricRegistry metricRegistry,
            ObjectMapper mapper,
            Executor bossPool,
            Provider<Executor> workerPoolProvider,
            ConnectionCounter connectionCounter) {
        super(configuration, throughputCounter, metricRegistry, mapper);
        this.bossExecutor = bossPool;
        this.workerExecutor = workerPoolProvider.get();
        this.connectionCounter = connectionCounter;
    }

    @Override
    protected Bootstrap getBootstrap() {
        final ServerBootstrap bootstrap =
                new ServerBootstrap(new NioServerSocketChannelFactory(bossExecutor, workerExecutor));

        bootstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(8192));
        bootstrap.setOption("child.receiveBufferSize", getRecvBufferSize());

        return bootstrap;
    }

    @Override
    protected List<Pair<String, ? extends ChannelHandler>> getBaseChannelHandlers(MessageInput2 input) {
        final List<Pair<String, ? extends ChannelHandler>> baseChannelHandlers = super.getBaseChannelHandlers(input);
        baseChannelHandlers.add(Pair.of("connection-counter", connectionCounter));
        return baseChannelHandlers;
    }

    @Override
    public void setupMetrics(MessageInput2 input) {
        super.setupMetrics(input);
        metricRegistry.register(name(input.getUniqueReadableId(), "open_connections"), connectionCounter.gaugeCurrent());
        metricRegistry.register(name(input.getUniqueReadableId(), "total_connections"), connectionCounter.gaugeTotal());
    }

}
