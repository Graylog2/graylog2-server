package org.graylog2.metrics.jersey2;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Metered;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ResourceInfo;
import java.io.IOException;

public class MeteredMetricsFilter extends AbstractMetricsFilter {

    private final Meter meter;

    public MeteredMetricsFilter(MetricRegistry metricRegistry, ResourceInfo resourceInfo) {
        final Metered annotation = resourceInfo.getResourceMethod().getAnnotation(Metered.class);
        meter = metricRegistry.meter(chooseName(annotation.name(), annotation.absolute(), resourceInfo.getResourceMethod()));
    }
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        meter.mark();
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        // nothing to do, we are just counting
    }
}
