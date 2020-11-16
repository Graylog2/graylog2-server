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

import org.graylog2.plugin.ServerStatus;

import javax.inject.Inject;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;

public class RestrictToMasterFeature implements DynamicFeature {
    private final ServerStatus serverStatus;
    private final RestrictToMasterFilter restrictToMasterFilter;

    @Inject
    public RestrictToMasterFeature(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
        this.restrictToMasterFilter = new RestrictToMasterFilter();
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        final Class<?> resourceClass = resourceInfo.getResourceClass();
        final Method resourceMethod = resourceInfo.getResourceMethod();

        if (serverStatus.hasCapability(ServerStatus.Capability.MASTER))
            return;

        if (resourceMethod.isAnnotationPresent(RestrictToMaster.class) || resourceClass.isAnnotationPresent(RestrictToMaster.class)) {
            context.register(restrictToMasterFilter);
        }
    }
}
