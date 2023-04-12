package org.graylog2.shared.rest.resources.annotations;

import org.graylog2.shared.metrics.jersey2.MetricsDynamicBinding;
import org.graylog2.shared.rest.CSPResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;

public class CSPDynamicFeature implements DynamicFeature {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsDynamicBinding.class);

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        final Method resourceMethod = resourceInfo.getResourceMethod();
        final Class<?> resourceClass = resourceInfo.getResourceClass();
        String cspValue = null;
        if (resourceClass != null && resourceClass.isAnnotationPresent(CSP.class)) {
            cspValue = resourceClass.getAnnotation(CSP.class).value();
            LOG.info("CSP class annotation: {}", cspValue);
        } else if (resourceMethod != null && resourceMethod.isAnnotationPresent(CSP.class)) {
            cspValue = resourceMethod.getAnnotation(CSP.class).value();
            LOG.info("CSP method annotation: {}", cspValue);
        }
        if (cspValue != null) {
            CSPResponseFilter filter = new CSPResponseFilter(cspValue);
            context.register(filter);
        }
    }
}
