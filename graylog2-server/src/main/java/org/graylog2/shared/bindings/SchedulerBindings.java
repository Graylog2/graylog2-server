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
package org.graylog2.shared.bindings;

import com.codahale.metrics.InstrumentedScheduledExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog2.periodical.Periodicals;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class SchedulerBindings extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(SchedulerBindings.class);
    private static final int SCHEDULED_THREADS_POOL_SIZE = 30;

    @Override
    protected void configure() {

        bind(ScheduledExecutorService.class).annotatedWith(Names.named("scheduler"))
                .toProvider(SchedulerProvider.class)
                .asEagerSingleton();

        bind(ScheduledExecutorService.class).annotatedWith(Names.named("daemonScheduler"))
                .toProvider(DaemonSchedulerProvider.class)
                .asEagerSingleton();

        bind(Periodicals.class).asEagerSingleton();
    }

    private static InstrumentedScheduledExecutorService buildScheduledExecutorService(String name,
                                                                                      boolean daemon,
                                                                                      MetricRegistry metricRegistry) {
        final var metricsPrefix = "org.graylog2.shared-thread-pools." + name;
        final var threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(name + "-%d")
                .setDaemon(daemon)
                .setUncaughtExceptionHandler(new Tools.LogUncaughtExceptionHandler(LOG))
                .build();
        final var executor = new ScheduledThreadPoolExecutor(SCHEDULED_THREADS_POOL_SIZE, threadFactory);
        return new InstrumentedScheduledExecutorService(executor, metricRegistry, metricsPrefix + "-executor");
    }

    public static class SchedulerProvider implements Provider<ScheduledExecutorService> {
        private final MetricRegistry metricRegistry;

        @Inject
        public SchedulerProvider(MetricRegistry metricRegistry) {
            this.metricRegistry = metricRegistry;
        }

        @Override
        public ScheduledExecutorService get() {
            return buildScheduledExecutorService("scheduled", false, metricRegistry);
        }
    }

    public static class DaemonSchedulerProvider implements Provider<ScheduledExecutorService> {
        private final MetricRegistry metricRegistry;

        @Inject
        public DaemonSchedulerProvider(MetricRegistry metricRegistry) {
            this.metricRegistry = metricRegistry;
        }

        @Override
        public ScheduledExecutorService get() {
            return buildScheduledExecutorService("scheduled-daemon", true, metricRegistry);
        }

    }
}
