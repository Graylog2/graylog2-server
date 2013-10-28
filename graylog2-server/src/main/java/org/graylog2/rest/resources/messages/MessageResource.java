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

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.elasticsearch.indices.IndexMissingException;
import org.graylog2.indexer.messages.DocumentNotFoundException;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Api(value = "Messages", description = "Single messages")
@Path("/messages/{index}")
public class MessageResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(MessageResource.class);

    @GET @Path("/{messageId}") @Timed
    @ApiOperation(value = "Get a single message.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Specified index does not exist."),
            @ApiResponse(code = 404, message = "Message does not exist.")
    })
    public String search(
            @ApiParam(title = "index", description = "The index this message is stored in.", required = true) @PathParam("index") String index,
            @ApiParam(title = "messageId", required = true) @PathParam("messageId") String messageId) {
        if (messageId == null || messageId.isEmpty()) {
        	LOG.error("Missing parameters. Returning HTTP 400.");
        	throw new WebApplicationException(400);
        }

		try {
			return json(core.getIndexer().messages().get(messageId, index));
		} catch (IndexMissingException e) {
        	LOG.error("Index does not exist. Returning HTTP 404.");
        	throw new WebApplicationException(404);
		} catch (DocumentNotFoundException e1) {
        	LOG.error("Message does not exist. Returning HTTP 404.");
        	throw new WebApplicationException(404);
		}
    }
    
    @GET @Path("/analyze") @Timed
    @ApiOperation(value = "Analyze a message string",
                  notes = "Returns what tokens/terms a message string (message or full_message) is split to.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Specified index does not exist."),
    })
    public String analyze(
            @ApiParam(title = "index", description = "The index the message containing the string is stored in.", required = true) @PathParam("index") String index,
            @ApiParam(title = "string", description = "The string to analyze.", required = true) @QueryParam("string") String string) {
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

        return json(result);
    }

}
