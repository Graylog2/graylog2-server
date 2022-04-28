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

import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import org.graylog.metrics.prometheus.mapping.InputMetricMapping;
import org.graylog.metrics.prometheus.mapping.MetricMapping;
import org.graylog.metrics.prometheus.mapping.MetricMatchMapping;
import org.graylog2.plugin.PluginModule;

public class PrometheusMetricsModule extends PluginModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(PrometheusExporterHTTPServer.Factory.class));

        final MapBinder<String, MetricMapping.Factory<? extends MetricMapping>> mappingBinder =
                MapBinder.newMapBinder(binder(),
                        TypeLiteral.get(String.class),
                        new TypeLiteral<MetricMapping.Factory<? extends MetricMapping>>() {}
                );

        install(new FactoryModuleBuilder().implement(MetricMapping.class, MetricMatchMapping.class)
                .build(MetricMatchMapping.Factory.class));
        mappingBinder.addBinding(MetricMatchMapping.TYPE).to(MetricMatchMapping.Factory.class);

        install(new FactoryModuleBuilder().implement(MetricMapping.class, InputMetricMapping.class)
                .build(InputMetricMapping.Factory.class));
        mappingBinder.addBinding(InputMetricMapping.TYPE).to(InputMetricMapping.Factory.class);

        addInitializer(PrometheusExporter.class);
    }
}
