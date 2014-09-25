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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Provider
@Priority(Integer.MIN_VALUE)
public class TimedMetricsFilter extends AbstractMetricsFilter {

    private final Timer timer;

    public TimedMetricsFilter(MetricRegistry metricRegistry, ResourceInfo resourceInfo) {
        final Timed annotation = resourceInfo.getResourceMethod().getAnnotation(Timed.class);
        timer = metricRegistry.timer(chooseName(annotation.name(), annotation.absolute(), resourceInfo.getResourceMethod()));
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        requestContext.setProperty("metricsTimerContext", timer.time());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        final Timer.Context context = (Timer.Context) requestContext.getProperty("metricsTimerContext");
        if (context == null) return;
        final long elapsedNanos = context.stop();
        responseContext.getHeaders().add("X-Runtime-Microseconds", TimeUnit.NANOSECONDS.toMicros(elapsedNanos));
    }
}
