package org.graylog2.rest.resources.count;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
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

import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.graylog2.Core;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.results.DateHistogramResult;
import org.graylog2.rest.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.sun.jersey.api.core.ResourceConfig;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/count")
public class CountResource extends RestResource {
	
    private static final Logger LOG = LoggerFactory.getLogger(CountResource.class);
	
    @Context ResourceConfig rc;
	
    @GET @Path("/total")
    @Produces(MediaType.APPLICATION_JSON)
    public String histogram(@QueryParam("interval") String interval, @QueryParam("timerange") int timerange, @QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");
        interval = interval.toUpperCase();

        if (timerange <= 0) {
        	LOG.error("Invalid timerange. Returning HTTP 400.");
        	throw new WebApplicationException(400);
        }
        
        try {
        	Indexer.DateHistogramInterval.valueOf(interval);
        } catch (IllegalArgumentException e) {
        	LOG.error("Invalid interval type. Returning HTTP 400.");
        	throw new WebApplicationException(400);
        }
        
        DateHistogramResult dhr = core.getIndexer().counts().totalCount(Indexer.DateHistogramInterval.valueOf(interval), timerange);

        Map<String, Object> result = Maps.newHashMap();
        result.put("query", dhr.getOriginalQuery());
        result.put("interval", dhr.getInterval().toString().toLowerCase());
        result.put("results", dhr.getResults());
        result.put("time", dhr.took().millis());
        
        return json(result, prettyPrint);
    }
}
