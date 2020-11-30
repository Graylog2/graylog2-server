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
package org.graylog2.shared.metrics.jersey2;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Provider
@Priority(Integer.MIN_VALUE)
public class TimedMetricsFilter extends AbstractMetricsFilter {

    private final Timer timer;

    public TimedMetricsFilter(MetricRegistry metricRegistry, ResourceInfo resourceInfo) {
        final Timed annotation = resourceInfo.getResourceMethod().getAnnotation(Timed.class);
        timer = metricRegistry.timer(chooseName(annotation.name(), annotation.absolute(), resourceInfo.getResourceMethod()));
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        requestContext.setProperty("metricsTimerContext", timer.time());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        final Timer.Context context = (Timer.Context) requestContext.getProperty("metricsTimerContext");
        if (context == null) return;
        final long elapsedNanos = context.stop();
        responseContext.getHeaders().add("X-Runtime-Microseconds", TimeUnit.NANOSECONDS.toMicros(elapsedNanos));
    }
}
