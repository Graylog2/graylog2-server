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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.ExceptionMetered;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ResourceInfo;
import java.io.IOException;

public class ExceptionMeteredMetricsFilter extends AbstractMetricsFilter {
    private final Meter meter;
    private final Class<? extends Throwable> exceptionClass;

    public ExceptionMeteredMetricsFilter(MetricRegistry metricRegistry, ResourceInfo resourceInfo) {
        final ExceptionMetered annotation = resourceInfo.getResourceMethod().getAnnotation(ExceptionMetered.class);
        meter = metricRegistry.meter(chooseName(annotation.name(), annotation.absolute(), resourceInfo.getResourceMethod(), ExceptionMetered.DEFAULT_NAME_SUFFIX));
        exceptionClass = annotation.cause();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // nothing to do, we are counting exceptions after the request was handled
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (responseContext.hasEntity()) {
            Exception e = (Exception) responseContext.getEntity();
            if (exceptionClass.isAssignableFrom(e.getClass()) ||
                    (e.getCause() != null && exceptionClass.isAssignableFrom(e.getCause().getClass()))) {
                meter.mark();
            }
            responseContext.setEntity(null);
            responseContext.getHeaders().add("X-Exceptions-Thrown", e.toString() + " : " + meter.getCount());
        }
    }
}
