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
package org.graylog.metrics.prometheus;

import com.codahale.metrics.MetricRegistry;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractIdleService;
import io.prometheus.client.Collector;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.dropwizard.samplebuilder.CustomMappingSampleBuilder;
import io.prometheus.client.dropwizard.samplebuilder.MapperConfig;
import org.graylog.metrics.prometheus.mapping.PrometheusMappingFilesHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Singleton
public class PrometheusExporter extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(PrometheusExporter.class);

    private final boolean enabled;
    private final ScheduledExecutorService scheduler;
    private final MetricRegistry metricRegistry;
    private final Provider<PrometheusMappingFilesHandler> mappingFilesHandlerProvider;
    private final PrometheusExporterHTTPServer httpServer;
    private final long refreshIntervalMs;

    private ScheduledFuture<?> refreshFuture;
    private PrometheusMappingFilesHandler mappingFilesHandler;

    @Inject
    public PrometheusExporter(@Named(PrometheusExporterConfiguration.ENABLED) boolean enabled,
                              @Named(PrometheusExporterConfiguration.BIND_ADDRESS) HostAndPort bindAddress,
                              @Named(PrometheusExporterConfiguration.MAPPING_FILE_REFRESH_INTERVAL) Duration mappingFileRefreshInterval,
                              @Named("daemonScheduler") ScheduledExecutorService scheduler,
                              MetricRegistry metricRegistry,
                              Provider<PrometheusMappingFilesHandler> mappingFilesHandlerProvider,
                              PrometheusExporterHTTPServer.Factory httpServerFactory) {
        this.enabled = enabled;
        this.scheduler = scheduler;
        this.metricRegistry = metricRegistry;
        this.mappingFilesHandlerProvider = mappingFilesHandlerProvider;
        this.httpServer = httpServerFactory.create(bindAddress);
        this.refreshIntervalMs = mappingFileRefreshInterval.toMilliseconds();
    }

    @Override
    protected void startUp() throws Exception {
        if (!enabled) {
            LOG.debug("Exporter disabled");
            return;
        }

        createMappingFilesHandler();

        httpServer.replaceCollector(createCollector(mappingFilesHandler.getMapperConfigs()));
        httpServer.start();

        this.refreshFuture = scheduler.scheduleAtFixedRate(this::refresh, refreshIntervalMs, refreshIntervalMs, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void shutDown() throws Exception {
        httpServer.stop();

        if (refreshFuture != null) {
            refreshFuture.cancel(true);
        }
    }

    private void refresh() {
        if (!enabled) {
            LOG.debug("Exporter disabled or HTTP server not running, no need to refresh the mappings.");
            return;
        }

        try {
            if (mappingFilesHandler.filesHaveChanged()) {
                httpServer.replaceCollector(createCollector(mappingFilesHandler.getMapperConfigs()));
            }
        } catch (Exception e) {
            LOG.error("Couldn't refresh mapping files", e);
        }
    }

    private Collector createCollector(List<MapperConfig> mapperConfigs) {
        return new DropwizardExports(
                metricRegistry,
                new PrometheusMetricFilter(mapperConfigs),
                new CustomMappingSampleBuilder(mapperConfigs)
        );
    }

    public Collector createCollector() {
        createMappingFilesHandler();
        return createCollector(this.mappingFilesHandler.getMapperConfigs());
    }

    private void createMappingFilesHandler() {
        if (Objects.isNull(mappingFilesHandler)) {
            this.mappingFilesHandler = mappingFilesHandlerProvider.get();
        }
    }
}
