package org.graylog2.metrics.jersey2;

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
    private static final Logger log = LoggerFactory.getLogger(ExceptionMeteredMetricsFilter.class);
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
