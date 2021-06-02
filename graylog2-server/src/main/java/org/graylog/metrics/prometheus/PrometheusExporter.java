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
import com.google.common.io.Resources;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractIdleService;
import io.prometheus.client.Collector;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.dropwizard.samplebuilder.CustomMappingSampleBuilder;
import io.prometheus.client.dropwizard.samplebuilder.MapperConfig;
import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;
import org.graylog.metrics.prometheus.mapping.PrometheusMappingFilesHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class PrometheusExporter extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(PrometheusExporter.class);
    private static final String CORE_MAPPING_RESOURCE = "prometheus-exporter.yml";

    private final boolean enabled;
    private final Path coreMappingPath;
    private final Path customMappingPath;
    private final ScheduledExecutorService scheduler;
    private final MetricRegistry metricRegistry;
    private final PrometheusMappingFilesHandler.Factory mappingFilesHandlerFactory;
    private final PrometheusExporterHTTPServer httpServer;
    private final long refreshIntervalMs;

    private ScheduledFuture<?> refreshFuture;
    private PrometheusMappingFilesHandler mappingFilesHandler;

    @Inject
    public PrometheusExporter(@Named(PrometheusExporterConfiguration.ENABLED) boolean enabled,
                              @Named(PrometheusExporterConfiguration.BIND_ADDRESS) HostAndPort bindAddress,
                              @Named(PrometheusExporterConfiguration.MAPPING_FILE_PATH_CORE) Path coreMappingPath,
                              @Named(PrometheusExporterConfiguration.MAPPING_FILE_PATH_CUSTOM) Path customMappingPath,
                              @Named(PrometheusExporterConfiguration.MAPPING_FILE_REFRESH_INTERVAL) Duration mappingFileRefreshInterval,
                              @Named("daemonScheduler") ScheduledExecutorService scheduler,
                              MetricRegistry metricRegistry,
                              PrometheusMappingFilesHandler.Factory mappingFilesHandlerFactory,
                              PrometheusExporterHTTPServer.Factory httpServerFactory) {
        this.enabled = enabled;
        this.coreMappingPath = coreMappingPath;
        this.customMappingPath = customMappingPath;
        this.scheduler = scheduler;
        this.metricRegistry = metricRegistry;
        this.mappingFilesHandlerFactory = mappingFilesHandlerFactory;
        this.httpServer = httpServerFactory.create(bindAddress);
        this.refreshIntervalMs = mappingFileRefreshInterval.toMilliseconds();
    }

    @Override
    protected void startUp() throws Exception {
        if (!enabled) {
            LOG.info("Exporter disabled - skipping");
            return;
        }

        final URL coreResource = Resources.getResource(CORE_MAPPING_RESOURCE);
        this.mappingFilesHandler = mappingFilesHandlerFactory.create(coreResource, coreMappingPath, customMappingPath);

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
        final List<Pattern> prometheusMetricPatterns = mapperConfigs.stream()
                .map(mc -> globToRegex(mc.getMatch()))
                .collect(Collectors.toList());
        final SampleBuilder sampleBuilder = new CustomMappingSampleBuilder(mapperConfigs);
        return new DropwizardExports(
                metricRegistry,
                (name, metric) -> prometheusMetricPatterns.stream().anyMatch(pattern -> pattern.matcher(name).matches()),
                sampleBuilder
        );
    }

    /**
     * Converts the given glob pattern (see {@link MapperConfig}) to a regexp {@link Pattern}.
     *
     * @param glob the glob pattern
     * @return regexp pattern
     */
    private Pattern globToRegex(String glob) {
        final String[] parts = glob.split(Pattern.quote("*"), -1);
        final StringBuilder escapedPattern = new StringBuilder(Pattern.quote(parts[0]));

        for (int i = 1; i < parts.length; i++) {
            escapedPattern.append("([^.]*)").append(Pattern.quote(parts[i]));
        }

        return Pattern.compile("^" + escapedPattern + "$");
    }
}
