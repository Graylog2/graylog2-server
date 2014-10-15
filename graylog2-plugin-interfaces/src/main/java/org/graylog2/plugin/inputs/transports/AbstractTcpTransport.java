/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.inputs.transports;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.collections.Pair;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput2;
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
            Executor bossPool,
            Provider<Executor> workerPoolProvider,
            ConnectionCounter connectionCounter) {
        super(configuration, throughputCounter, metricRegistry);
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
