package org.graylog.metrics.prometheus;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog.metrics.prometheus.mapping.PrometheusMappingFilesHandler;
import org.graylog2.plugin.PluginModule;

public class PrometheusMetricsModule extends PluginModule {
    @Override
    protected void configure() {
        //addSystemRestResource();

        install(new FactoryModuleBuilder().build(PrometheusMappingFilesHandler.Factory.class));
        install(new FactoryModuleBuilder().build(PrometheusExporterHTTPServer.Factory.class));

        addInitializer(PrometheusExporter.class);
    }
}
