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
package org.graylog2.shared.metrics.jersey2;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.ExceptionMetered;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ResourceInfo;
import java.io.IOException;

public class ExceptionMeteredMetricsFilter extends AbstractMetricsFilter {
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionMeteredMetricsFilter.class);
    private final Meter meter;
    private final Class<? extends Throwable> exceptionClass;

    public ExceptionMeteredMetricsFilter(MetricRegistry metricRegistry, ResourceInfo resourceInfo) {
        final ExceptionMetered annotation = resourceInfo.getResourceMethod().getAnnotation(ExceptionMetered.class);
        meter = metricRegistry.meter(chooseName(annotation.name(), annotation.absolute(), resourceInfo.getResourceMethod(), ExceptionMetered.DEFAULT_NAME_SUFFIX));
        exceptionClass = annotation.cause();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // nothing to do, we are counting exceptions after the request was handled
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (responseContext.hasEntity()) {
            Exception e = (Exception) responseContext.getEntity();
            if (exceptionClass.isAssignableFrom(e.getClass()) ||
                    (e.getCause() != null && exceptionClass.isAssignableFrom(e.getCause().getClass()))) {
                meter.mark();
            }
            responseContext.setEntity(null);
            responseContext.getHeaders().add("X-Exceptions-Thrown", e.toString() + " : " + meter.getCount());
        }
    }
}
