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
package org.graylog2.initializers;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.Configuration;
import org.graylog2.buffers.Buffers;
import org.graylog2.indexer.cluster.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

@Singleton
public class BufferSynchronizerService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(BufferSynchronizerService.class);

    private final Buffers bufferSynchronizer;
    private final Cluster cluster;
    private final Configuration configuration;
    private final MetricRegistry metricRegistry;

    @Inject
    public BufferSynchronizerService(final Buffers buffers,
                                     final Cluster cluster,
                                     final Configuration configuration,
                                     final MetricRegistry metricRegistry) {
        this.bufferSynchronizer = buffers;
        this.cluster = cluster;
        this.configuration = configuration;
        this.metricRegistry = metricRegistry;
    }

    @Override
    protected void startUp() throws Exception {
    }

    @Override
    protected void shutDown() throws Exception {
        LOG.debug("Stopping BufferSynchronizerService");
        if (cluster.isConnected() && cluster.isDeflectorHealthy()) {
            final ExecutorService executorService = executorService(metricRegistry);

            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    bufferSynchronizer.waitForEmptyBuffers(configuration.getShutdownTimeout(), TimeUnit.MILLISECONDS);
                }
            });

            executorService.shutdown();
            executorService.awaitTermination(configuration.getShutdownTimeout(), TimeUnit.MILLISECONDS);
        } else {
            LOG.warn("Elasticsearch is unavailable. Not waiting to clear buffers and caches, as we have no healthy cluster.");
        }
        LOG.debug("Stopped BufferSynchronizerService");
    }

    private ExecutorService executorService(MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("buffer-synchronizer-%d").build();
        return new InstrumentedExecutorService(
                Executors.newSingleThreadExecutor(threadFactory),
                metricRegistry,
                name(this.getClass(), "executor-service"));
    }
}
