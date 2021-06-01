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

        this.mappingFilesHandler = mappingFilesHandlerFactory.create(Resources.getResource("prometheus-exporter-dev.yml"), coreMappingPath, customMappingPath);

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
                .map(mc -> Pattern.compile(mc.getMatch()))
                .collect(Collectors.toList());
        final SampleBuilder sampleBuilder = new CustomMappingSampleBuilder(mapperConfigs);
        return new DropwizardExports(
                metricRegistry,
                (name, metric) -> prometheusMetricPatterns.stream().anyMatch(pattern -> pattern.matcher(name).matches()),
                sampleBuilder
        );
    }
}
