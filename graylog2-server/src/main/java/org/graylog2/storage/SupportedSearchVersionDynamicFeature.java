/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.storage;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;

public class SupportedSearchVersionDynamicFeature implements DynamicFeature {
    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        final Method resourceMethod = resourceInfo.getResourceMethod();
        final Class resourceClass = resourceInfo.getResourceClass();
        if ((resourceMethod != null && (resourceMethod.isAnnotationPresent(SupportedSearchVersion.class)
                || resourceMethod.isAnnotationPresent(SupportedSearchVersions.class)))
                || (resourceClass != null && (resourceClass.isAnnotationPresent(SupportedSearchVersion.class)
                || resourceClass.isAnnotationPresent(SupportedSearchVersions.class)))) {
            context.register(SupportedSearchVersionFilter.class);
        }
    }
}
