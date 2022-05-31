package org.graylog.metrics.prometheus;

import com.codahale.metrics.MetricRegistry;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.net.HostAndPort;
import io.prometheus.client.Collector;
import io.prometheus.client.dropwizard.samplebuilder.MapperConfig;
import org.graylog.metrics.prometheus.mapping.PrometheusMappingFilesHandler;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.system.stats.jvm.JvmStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.inject.Provider;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class PrometheusExporterTest {

    private MetricRegistry metricRegistry;
    @Mock
    private Provider<PrometheusMappingFilesHandler> prometheusMappingFilesHandlerProvider;
    @Mock
    private PrometheusMappingFilesHandler prometheusMappingFilesHandler;
    @Mock
    private ScheduledExecutorService scheduledExecutorService;
    @Mock
    private  PrometheusExporterHTTPServer.Factory factory;

    private PrometheusExporter classUnderTest;


    @BeforeEach
    void init() {
        metricRegistry = new LocalMetricRegistry();
        classUnderTest = new PrometheusExporter(false, HostAndPort.fromParts("localhost", 8080), Duration.seconds(60), scheduledExecutorService, metricRegistry, prometheusMappingFilesHandlerProvider, factory);
    }

    @Test
    void testCreateCollector() {
        when(prometheusMappingFilesHandlerProvider.get()).thenReturn(prometheusMappingFilesHandler);
        when(prometheusMappingFilesHandler.getMapperConfigs()).thenReturn(Collections.singletonList(new MapperConfig(
                "org.graylog2.plugin.streams.Stream.*.StreamRule.*.executionTime",
                "stream_rules_execution_time",
                Collections.EMPTY_MAP
        )));

        metricRegistry.timer(MetricRegistry.name(Stream.class, "stream-id", "StreamRule", "stream-rule-id",
                "executionTime"));
        metricRegistry.timer(MetricRegistry.name(JvmStats.class, "jvm-stats"));

        Collector collector = classUnderTest.createCollector();

        assertThat(collector.collect().size()).isEqualTo(1);
    }
}
