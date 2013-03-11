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

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.graylog2.Core;
import org.graylog2.indexer.searches.SearchResult;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jersey.api.core.ResourceConfig;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/search")
public class SearchResource {
    @Context ResourceConfig rc;

    @GET @Path("/universal")
    @Produces(MediaType.APPLICATION_JSON)
    public String system(@QueryParam("query") String query, @QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        if (query == null || query.isEmpty()) {
        	throw new WebApplicationException(400);
        }
        
        SearchResult sr = core.getIndexer().searches().universalSearch(query);
        
        Gson gson = new Gson();
        
        if (prettyPrint) {
            gson = new GsonBuilder().setPrettyPrinting().create();
        }
        
        Map<String, Object> result = Maps.newHashMap();
        result.put("query", sr.getOriginalQuery());
        result.put("messages", sr.getResults());
        result.put("time", sr.took().millis());
        result.put("total_results", sr.getTotalResults());
        
        return gson.toJson(result);
    }
}