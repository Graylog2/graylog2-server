/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
