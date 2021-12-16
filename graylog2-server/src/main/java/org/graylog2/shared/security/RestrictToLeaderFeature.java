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
package org.graylog2.shared.security;

import javax.inject.Inject;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;

public class RestrictToLeaderFeature implements DynamicFeature {
    private final RestrictToLeaderFilter restrictToLeaderFilter;

    @Inject
    public RestrictToLeaderFeature(RestrictToLeaderFilter restrictToLeaderFilter) {
        this.restrictToLeaderFilter = restrictToLeaderFilter;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        final Class<?> resourceClass = resourceInfo.getResourceClass();
        final Method resourceMethod = resourceInfo.getResourceMethod();

        //noinspection deprecation
        if (resourceMethod.isAnnotationPresent(RestrictToLeader.class) ||
                resourceMethod.isAnnotationPresent(RestrictToMaster.class) ||
                resourceClass.isAnnotationPresent(RestrictToLeader.class) ||
                resourceClass.isAnnotationPresent(RestrictToMaster.class)) {
            context.register(restrictToLeaderFilter);
        }
    }
}
