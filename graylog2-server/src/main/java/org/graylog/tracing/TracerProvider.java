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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

import jakarta.inject.Provider;
import jakarta.inject.Singleton;

/**
 * Relies on the opentelemetry javaagent to provide an implementation of a tracer. If the javaagent is not present,
 * this provider will supply a no-op tracer.
 */
@Singleton
public class TracerProvider implements Provider<Tracer> {

    @Override
    public Tracer get() {
        return GlobalOpenTelemetry.get().getTracer("org.graylog");
    }

    public static Tracer noop() {
        return io.opentelemetry.api.trace.TracerProvider.noop().get("org.graylog");
    }
}
