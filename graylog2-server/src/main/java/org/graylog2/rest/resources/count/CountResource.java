/**
 * Copyright 2013 Lennart Koopmann <lennart@socketfeed.com>
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
package org.graylog2.rest.resources.count;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.results.DateHistogramResult;
import org.graylog2.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/count")
public class CountResource extends RestResource {
	
    private static final Logger LOG = LoggerFactory.getLogger(CountResource.class);

    @GET @Path("/total") @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String total(@QueryParam("pretty") boolean prettyPrint) {
        Map<String, Long> result = Maps.newHashMap();
        result.put("events", core.getIndexer().counts().total());

        return json(result);
    }

    @GET @Path("/histogram") @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String histogram(@QueryParam("interval") String interval, @QueryParam("timerange") int timerange) {
        if (interval == null || interval.isEmpty()) {
            LOG.error("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        if (timerange <= 0) {
        	LOG.error("Invalid timerange. Returning HTTP 400.");
        	throw new WebApplicationException(400);
        }

        interval = interval.toUpperCase();
        try {
        	Indexer.DateHistogramInterval.valueOf(interval);
        } catch (IllegalArgumentException e) {
        	LOG.error("Invalid interval type. Returning HTTP 400.");
        	throw new WebApplicationException(400);
        }
        
        DateHistogramResult dhr = core.getIndexer().counts().histogram(Indexer.DateHistogramInterval.valueOf(interval), timerange);

        // TODO: Replace with Jackson JAX-RS provider and proper data binding
        Map<String, Object> result = Maps.newHashMap();
        result.put("query", dhr.getOriginalQuery());
        result.put("interval", dhr.getInterval().toString().toLowerCase());
        result.put("results", dhr.getResults());
        result.put("time", dhr.took().millis());

        return json(result);
    }
}
