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
package org.graylog2.shared.metrics.jersey2;

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
    private static final Logger LOG = LoggerFactory.getLogger(MetricsDynamicBinding.class);
    private final MetricRegistry metricRegistry;

    public MetricsDynamicBinding(@Context MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {

        final Method resourceMethod = resourceInfo.getResourceMethod();
        if (resourceMethod.isAnnotationPresent(Timed.class)) {
            LOG.debug("Setting up filter for Timed resource method: {}#{}", resourceInfo.getResourceClass().getCanonicalName(), resourceMethod.getName());
            context.register(new TimedMetricsFilter(metricRegistry, resourceInfo));
        }
        if (resourceMethod.isAnnotationPresent(Metered.class)) {
            LOG.debug("Setting up filter for Metered resource method: {}#{}", resourceInfo.getResourceClass().getCanonicalName(), resourceMethod.getName());
            context.register(new MeteredMetricsFilter(metricRegistry, resourceInfo));
        }
        if (resourceMethod.isAnnotationPresent(ExceptionMetered.class)) {
            LOG.debug("Setting up filter for ExceptionMetered resource method: {}#{}", resourceInfo.getResourceClass().getCanonicalName(), resourceMethod.getName());
            context.register(new ExceptionMeteredMetricsFilter(metricRegistry, resourceInfo));
        }
    }
}
