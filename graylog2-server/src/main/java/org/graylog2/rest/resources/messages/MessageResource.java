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

package org.graylog2.rest.resources.messages;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.core.ResourceConfig;
import org.elasticsearch.indices.IndexMissingException;
import org.graylog2.Core;
import org.graylog2.indexer.messages.DocumentNotFoundException;
import org.graylog2.indexer.results.ResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.indices.IndexMissingException;
import org.graylog2.Core;
import org.graylog2.indexer.messages.DocumentNotFoundException;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.rest.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.sun.jersey.api.core.ResourceConfig;
import javax.ws.rs.core.Response;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/messages/{index}")
public class MessageResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(MessageResource.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
	
    @Context ResourceConfig rc;

    @GET @Path("/{messageId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String search(@PathParam("index") String index, @PathParam("messageId") String messageId, @QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        if (messageId == null || messageId.isEmpty()) {
        	LOG.error("Missing parameters. Returning HTTP 400.");
        	throw new WebApplicationException(400);
        }
        
        ResultMessage m;
		try {
			m = core.getIndexer().messages().get(messageId, index);
		} catch (IndexMissingException e) {
        	LOG.error("Index does not exist. Returning HTTP 404.");
        	throw new WebApplicationException(404);
		} catch (DocumentNotFoundException e) {
        	LOG.error("Message does not exist. Returning HTTP 404.");
        	throw new WebApplicationException(404);
		}

        return json(m, prettyPrint);
    }
    
    @GET @Path("/analyze")
    @Produces(MediaType.APPLICATION_JSON)
    public String analyze(@PathParam("index") String index, @QueryParam("string") String string, @QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        if (string == null || string.isEmpty()) {
        	LOG.error("Missing parameters. Returning HTTP 400.");
        	throw new WebApplicationException(400);
        }
        
        List<String> tokens;
        try {
        	tokens = core.getIndexer().messages().analyze(string, index);
		} catch (IndexMissingException e) {
        	LOG.error("Index does not exist. Returning HTTP 404.");
        	throw new WebApplicationException(404);
		}
        
        Map<String, Object> result = Maps.newHashMap();
        result.put("tokens", tokens);

        return json(result, prettyPrint);

        try {
            if (prettyPrint) {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(m);
            } else {
                return objectMapper.writeValueAsString(m);
            }
        } catch (JsonProcessingException e) {
            LOG.error("Error while generating JSON", e);
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
