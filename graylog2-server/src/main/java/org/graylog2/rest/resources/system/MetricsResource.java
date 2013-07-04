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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.Metric;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import com.sun.jersey.api.core.ResourceConfig;
import org.graylog2.Core;
import org.graylog2.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/system/metrics")
public class MetricsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsResource.class);

    @Context
    ResourceConfig rc;

    @GET
    @Timed
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public String metrics(@QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        Map<String, Object> result = Maps.newHashMap();
        result.put("metrics", core.metrics().getMetrics());

        return json(result, prettyPrint);
    }

    @GET
    @Timed
    @Path("/{metricName}")
    @Produces(MediaType.APPLICATION_JSON)
    public String singleMetric(@PathParam("metricName") String metricName, @QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        Metric metric = core.metrics().getMetrics().get(metricName);

        if (metric == null) {
            LOG.warn("I do not have a metric called [{}], returning 404.", metricName);
            throw new WebApplicationException(404);
        }

        return json(metric, prettyPrint);
    }

}
