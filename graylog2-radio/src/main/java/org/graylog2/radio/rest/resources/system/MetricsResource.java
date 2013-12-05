/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.radio.rest.resources.system;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.radio.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/system/metrics")
public class MetricsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsResource.class);

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String metrics() {
        Map<String, Object> result = Maps.newHashMap();

        result.put("metrics", radio.metrics().getMetrics());

        return json(result);
    }

    @GET @Timed
    @Path("/names")
    @Produces(MediaType.APPLICATION_JSON)
    public String metricNames() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("names", radio.metrics().getNames());

        return json(result);
    }

    @GET @Timed
    @Path("/{metricName}")
    @Produces(MediaType.APPLICATION_JSON)
    public String singleMetric(@PathParam("metricName") String metricName) {
        Metric metric = radio.metrics().getMetrics().get(metricName);

        if (metric == null) {
            LOG.debug("I do not have a metric called [{}], returning 404.", metricName);
            throw new WebApplicationException(404);
        }

        return json(metric);
    }

    @GET @Timed
    @Path("/namespace/{namespace}")
    @Produces(MediaType.APPLICATION_JSON)
    public String byNamespace(@PathParam("namespace") String namespace) {
        List<Map<String, Object>> metrics = Lists.newArrayList();

        for(Map.Entry<String, Metric> e : radio.metrics().getMetrics().entrySet()) {
            if (e.getKey().startsWith(namespace)) {
                try {
                    String type = e.getValue().getClass().getSimpleName().toLowerCase();
                    String metricName = e.getKey();

                    if (type.isEmpty()) {
                        type = "gauge";
                    }

                    Map<String, Object> metric = Maps.newHashMap();
                    metric.put("full_name", metricName);
                    metric.put("name", metricName.substring(metricName.lastIndexOf(".") + 1));
                    metric.put("type", type);

                    if (e.getValue() instanceof Timer) {
                        metric.put("metric", buildTimerMap((Timer) e.getValue()));
                    } else if(e.getValue() instanceof Meter) {
                        metric.put("metric", buildMeterMap((Meter) e.getValue()));
                    } else if(e.getValue() instanceof Histogram) {
                        metric.put("metric", buildHistogramMap((Histogram) e.getValue()));
                    } else {
                        metric.put("metric", e.getValue());
                    }

                    metrics.add(metric);
                } catch(Exception ex) {
                    LOG.warn("Could not read metric in namespace list.", ex);
                    continue;
                }
            }
        }

        if (metrics.isEmpty()) {
            LOG.debug("No metrics with namespace [{}] found, returning 404.", namespace);
            throw new WebApplicationException(404);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("metrics", metrics);
        result.put("total", metrics.size());

        return json(result);
    }

}