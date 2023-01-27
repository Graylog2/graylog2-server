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
package org.graylog.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

@Singleton
public class OpenTelemetryProvider implements Provider<OpenTelemetry> {
    private final TracingConfiguration configuration;

    @Inject
    public OpenTelemetryProvider(TracingConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public OpenTelemetry get() {
        var resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, configuration.getServiceName())));

        final SpanProcessor spanProcessor;
        if (configuration.isExporterEnabled()) {
            spanProcessor = BatchSpanProcessor.builder(JaegerGrpcSpanExporter.builder()
                            .setTimeout(configuration.getExporterPublishTimeout().toMilliseconds(), TimeUnit.MILLISECONDS)
                            .setEndpoint(configuration.getExporterHttpEndpoint().toString())
                            .build())
                    .build();
        } else {
            spanProcessor = new NoopSpanProcessor();
        }

        var sdkTraceProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(spanProcessor)
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTraceProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
    }
}
