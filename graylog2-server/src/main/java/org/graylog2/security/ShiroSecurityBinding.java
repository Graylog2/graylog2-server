/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.security;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.util.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.*;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;

/**
 * @author Kay Roepke <kay@torch.sh>
 */
public class ShiroSecurityBinding implements DynamicFeature {
    private static final Logger LOG = LoggerFactory.getLogger(ShiroSecurityBinding.class);

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        final Class<?> resourceClass = resourceInfo.getResourceClass();
        final Method resourceMethod = resourceInfo.getResourceMethod();

        if (resourceMethod.isAnnotationPresent(RequiresAuthentication.class) ||
                resourceClass.isAnnotationPresent(RequiresAuthentication.class)) {
            if (resourceMethod.isAnnotationPresent(RequiresGuest.class)) {
                LOG.debug("Resource method {}#{} is marked as unauthenticated, skipping setting filter.");
            } else {
                LOG.debug("Resource method {}#{} requires an authenticated user.", resourceClass.getCanonicalName(), resourceMethod.getName());
                context.register(new ShiroAuthenticationFilter());
            }
        }
        if (resourceMethod.isAnnotationPresent(RequiresPermissions.class) ||
                resourceClass.isAnnotationPresent(RequiresPermissions.class)) {
            RequiresPermissions a = resourceClass.getAnnotation(RequiresPermissions.class);
            if (a == null) {
                a = resourceMethod.getAnnotation(RequiresPermissions.class);
            }
            LOG.debug("Resource method {}#{} requires an authorization checks.", resourceClass.getCanonicalName(), resourceMethod.getName());
            context.register(new ShiroAuthorizationFilter(a));
        }
        // TODO this is the wrong approach, we should have an Environment and proper request wrapping
        context.register(new ContainerResponseFilter() {
            @Override
            public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
                ThreadContext.unbindSubject();
            }
        });
    }
}
