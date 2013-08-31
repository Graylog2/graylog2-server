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

package org.graylog2.rest.resources.search;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.results.DateHistogramResult;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.results.TermsResult;
import org.graylog2.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/search")
public class SearchResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SearchResource.class);

    @GET @Path("/universal") @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String search(@QueryParam("query") String query, @QueryParam("timerange") int timerange, @QueryParam("limit") int limit) {
        if (query == null || query.isEmpty()) {
        	LOG.error("Missing parameters. Returning HTTP 400.");
        	throw new WebApplicationException(400);
        }

        SearchResult sr = core.getIndexer().searches().search(query, timerange, limit);

        Map<String, Object> result = Maps.newHashMap();
        result.put("query", sr.getOriginalQuery());
        result.put("messages", sr.getResults());
        result.put("fields", sr.getFields());
        result.put("time", sr.took().millis());
        result.put("total_results", sr.getTotalResults());

        return json(result);
    }

    @GET @Path("/universal/terms") @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String terms(@QueryParam("field") String field, @QueryParam("query") String query, @QueryParam("size") int size, @QueryParam("timerange") int timerange) {
        if (field == null || field.isEmpty() || query == null || query.isEmpty()) {
            LOG.error("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        TermsResult tr = core.getIndexer().searches().terms(field, size, query, timerange);

        Map<String, Object> result = Maps.newHashMap();
        result.put("query", query);
        result.put("time", tr.took().millis());
        result.put("terms", tr.getTerms());
        result.put("missing", tr.getMissing()); // The number of docs missing a value.
        result.put("other", tr.getOther()); // The count of terms other than the one provided by the entries.
        result.put("total", tr.getTotal()); // The total count of terms.

        return json(result);
    }
    
    @GET @Path("/universal/histogram") @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String histogram(@QueryParam("query") String query, @QueryParam("interval") String interval, @QueryParam("timerange") int timerange) {
        interval = interval.toUpperCase();

        if (query == null || query.isEmpty() || interval == null || interval.isEmpty()) {
        	LOG.error("Missing parameters. Returning HTTP 400.");
        	throw new WebApplicationException(400);
        }
        
        try {
        	Indexer.DateHistogramInterval.valueOf(interval);
        } catch (IllegalArgumentException e) {
        	LOG.error("Invalid interval type. Returning HTTP 400.");
        	throw new WebApplicationException(400);
        }
        
        DateHistogramResult dhr = core.getIndexer().searches().histogram(query, Indexer.DateHistogramInterval.valueOf(interval), timerange);

        Map<String, Object> result = Maps.newHashMap();
        result.put("query", dhr.getOriginalQuery());
        result.put("interval", dhr.getInterval().toString().toLowerCase());
        result.put("results", dhr.getResults());
        result.put("time", dhr.took().millis());

        return json(result);
    }

}