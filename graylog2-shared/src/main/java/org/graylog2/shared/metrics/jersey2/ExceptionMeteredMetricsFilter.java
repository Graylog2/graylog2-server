/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.shared.metrics.jersey2;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.ExceptionMetered;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ResourceInfo;
import java.io.IOException;

public class ExceptionMeteredMetricsFilter extends AbstractMetricsFilter {
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionMeteredMetricsFilter.class);
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
