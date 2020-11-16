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

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.InstrumentedThreadFactory;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.graylog2.inputs.transports.NettyTransportConfiguration;
import org.graylog2.plugin.LocalMetricRegistry;

import javax.inject.Inject;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class EventLoopGroupFactory {
    private final NettyTransportConfiguration configuration;

    @Inject
    public EventLoopGroupFactory(NettyTransportConfiguration configuration) {
        this.configuration = configuration;
    }

    public EventLoopGroup create(int numThreads, MetricRegistry metricRegistry, String metricPrefix) {
        final ThreadFactory threadFactory = threadFactory(metricPrefix, metricRegistry);
        final Executor executor = executor(metricPrefix, numThreads, threadFactory, metricRegistry);

        switch (configuration.getType()) {
            case EPOLL:
                return epollEventLoopGroup(numThreads, executor);
            case KQUEUE:
                return kqueueEventLoopGroup(numThreads, executor);
            case NIO:
                return nioEventLoopGroup(numThreads, executor);
            default:
                throw new RuntimeException("Invalid or unknown netty transport type " + configuration.getType());
        }
    }

    private ThreadFactory threadFactory(String name, MetricRegistry metricRegistry) {
        final String threadFactoryMetricName = MetricRegistry.name(name, "thread-factory");
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("netty-transport-%d").build();
        return new InstrumentedThreadFactory(threadFactory, metricRegistry, threadFactoryMetricName);

    }

    private Executor executor(final String name, int numThreads, final ThreadFactory threadFactory, final MetricRegistry metricRegistry) {
        final String executorMetricName = LocalMetricRegistry.name(name, "executor-service");
        final ExecutorService cachedThreadPool = Executors.newFixedThreadPool(numThreads, threadFactory);
        return new InstrumentedExecutorService(cachedThreadPool, metricRegistry, executorMetricName);
    }

    private EventLoopGroup nioEventLoopGroup(int numThreads, Executor executor) {
        return new NioEventLoopGroup(numThreads, executor);
    }


    private EventLoopGroup epollEventLoopGroup(int numThreads, Executor executor) {
        return new EpollEventLoopGroup(numThreads, executor);
    }

    private EventLoopGroup kqueueEventLoopGroup(int numThreads, Executor executor) {
        return new KQueueEventLoopGroup(numThreads, executor);
    }
}
