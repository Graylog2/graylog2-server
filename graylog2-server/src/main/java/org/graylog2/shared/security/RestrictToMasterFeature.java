/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.security;

import org.graylog2.plugin.ServerStatus;

import javax.inject.Inject;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;

public class RestrictToParentFeature implements DynamicFeature {
    private final ServerStatus serverStatus;
    private final RestrictToParentFilter restrictToParentFilter;

    @Inject
    public RestrictToParentFeature(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
        this.restrictToParentFilter = new RestrictToParentFilter();
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        final Class<?> resourceClass = resourceInfo.getResourceClass();
        final Method resourceMethod = resourceInfo.getResourceMethod();

        if (serverStatus.hasCapability(ServerStatus.Capability.PARENT))
            return;

        if (resourceMethod.isAnnotationPresent(RestrictToParent.class) || resourceClass.isAnnotationPresent(RestrictToParent.class)) {
            context.register(restrictToParentFilter);
        }
    }
}
