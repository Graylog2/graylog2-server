package org.graylog2.metrics.jersey2;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;
import java.lang.reflect.Method;

import static com.codahale.metrics.MetricRegistry.name;

public abstract class AbstractMetricsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    protected String chooseName(String explicitName, boolean absolute, Method method, String... suffixes) {
        if (explicitName != null && !explicitName.isEmpty()) {
            if (absolute) {
                return explicitName;
            }
            return name(method.getDeclaringClass(), explicitName);
        }
        return name(name(method.getDeclaringClass(),
                method.getName()),
                suffixes);
    }

    @Override
    public abstract void filter(ContainerRequestContext requestContext) throws IOException;

    @Override
    public abstract void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException;
}
