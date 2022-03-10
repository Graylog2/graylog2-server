package org.graylog2.storage;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;

public class CheckSearchVersionDynamicFeature implements DynamicFeature {
    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        final Method resourceMethod = resourceInfo.getResourceMethod();
        final Class resourceClass = resourceInfo.getResourceClass();
        if ((resourceMethod != null && resourceMethod.isAnnotationPresent(RequiresSearchVersion.class))
                || (resourceClass != null && resourceClass.isAnnotationPresent(RequiresSearchVersion.class))) {
            context.register(CheckSearchVersionFilter.class);
        }
    }
}
