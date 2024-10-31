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
package org.graylog2.shared.bindings.providers;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;

@Singleton
public class ProxiedRequestsExecutorService implements Provider<ExecutorService> {
    private final int proxiedRequestsMaxThreads;
    private final MetricRegistry metricRegistry;

    @Inject
    public ProxiedRequestsExecutorService(@Named("proxied_requests_thread_pool_size") int proxiedRequestsMaxThreads,
                                          MetricRegistry metricRegistry) {
        this.proxiedRequestsMaxThreads = proxiedRequestsMaxThreads;
        this.metricRegistry = metricRegistry;
    }

    @Override
    public ExecutorService get() {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("proxied-requests-pool-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler(
                        new Tools.LogUncaughtExceptionHandler(LoggerFactory.getLogger(ProxiedResource.class.getName())))
                .build();
        return new InstrumentedExecutorService(
                Executors.newFixedThreadPool(proxiedRequestsMaxThreads, threadFactory),
                metricRegistry,
                name(this.getClass(), "http-proxied-requests-executor"));
    }
}
