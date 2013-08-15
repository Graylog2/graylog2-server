package org.graylog2.metrics.jersey2;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;

@Provider
public class MetricsDynamicBinding implements DynamicFeature {
    private static final Logger log = LoggerFactory.getLogger(MetricsDynamicBinding.class);
    private final MetricRegistry metricRegistry;

    public MetricsDynamicBinding(@Context MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {

        final Method resourceMethod = resourceInfo.getResourceMethod();
        if (resourceMethod.isAnnotationPresent(Timed.class)) {
            log.debug("Setting up filter for Timed resource method: {}#{}", resourceInfo.getResourceClass().getCanonicalName(), resourceMethod.getName());
            context.register(new TimedMetricsFilter(metricRegistry, resourceInfo));
        }
        if (resourceMethod.isAnnotationPresent(Metered.class)) {
            log.debug("Setting up filter for Metered resource method: {}#{}", resourceInfo.getResourceClass().getCanonicalName(), resourceMethod.getName());
            context.register(new MeteredMetricsFilter(metricRegistry, resourceInfo));
        }
        if (resourceMethod.isAnnotationPresent(ExceptionMetered.class)) {
            log.debug("Setting up filter for ExceptionMetered resource method: {}#{}", resourceInfo.getResourceClass().getCanonicalName(), resourceMethod.getName());
            context.register(new ExceptionMeteredMetricsFilter(metricRegistry, resourceInfo));
        }
    }
}
