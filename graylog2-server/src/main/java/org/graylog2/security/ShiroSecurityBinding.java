package org.graylog2.security;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;

public class ShiroSecurityBinding implements DynamicFeature {
    private static final Logger log = LoggerFactory.getLogger(ShiroSecurityBinding.class);

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        final Class<?> resourceClass = resourceInfo.getResourceClass();
        final Method resourceMethod = resourceInfo.getResourceMethod();

        if (resourceMethod.isAnnotationPresent(RequiresAuthentication.class) ||
                resourceClass.isAnnotationPresent(RequiresAuthentication.class)) {
            log.info("Resource method {}#{} requires an authenticated user.", resourceClass.getCanonicalName(), resourceMethod.getName());
            context.register(new ShiroAuthenticationFilter());
        }
    }
}
