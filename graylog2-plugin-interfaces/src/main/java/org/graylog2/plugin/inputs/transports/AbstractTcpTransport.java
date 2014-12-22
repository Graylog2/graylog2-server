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

import com.google.common.util.concurrent.Callables;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.util.ConnectionCounter;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public abstract class AbstractTcpTransport extends NettyTransport {
    protected final Executor bossExecutor;
    protected final Executor workerExecutor;
    protected final ConnectionCounter connectionCounter;

    public AbstractTcpTransport(
            Configuration configuration,
            ThroughputCounter throughputCounter,
            LocalMetricRegistry localRegistry,
            Executor bossPool,
            Executor workerPool,
            ConnectionCounter connectionCounter) {
        super(configuration, throughputCounter, localRegistry);
        this.bossExecutor = bossPool;
        this.workerExecutor = workerPool;
        this.connectionCounter = connectionCounter;

        this.localRegistry.register("open_connections", connectionCounter.gaugeCurrent());
        this.localRegistry.register("total_connections", connectionCounter.gaugeTotal());
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
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getBaseChannelHandlers(
            MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> baseChannelHandlers =
                super.getBaseChannelHandlers(input);
        baseChannelHandlers.put("connection-counter", Callables.returning(connectionCounter));
        return baseChannelHandlers;
    }

}
