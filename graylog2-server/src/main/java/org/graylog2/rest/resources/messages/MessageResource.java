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
package org.graylog2.rest.resources.messages;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.elasticsearch.indices.IndexMissingException;
import org.graylog2.indexer.messages.DocumentNotFoundException;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@RequiresAuthentication
@Api(value = "Messages", description = "Single messages")
@Path("/messages/{index}")
public class MessageResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(MessageResource.class);

    private Messages messages;

    @Inject
    public MessageResource(Messages messages) {
        this.messages = messages;
    }

    @GET
    @Path("/{messageId}")
    @Timed
    @ApiOperation(value = "Get a single message.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Specified index does not exist."),
            @ApiResponse(code = 404, message = "Message does not exist.")
    })
    public ResultMessage search(@ApiParam(name = "index", value = "The index this message is stored in.", required = true)
                                @PathParam("index") String index,
                                @ApiParam(name = "messageId", required = true)
                                @PathParam("messageId") String messageId) {
        checkPermission(RestPermissions.MESSAGES_READ, messageId);
        try {
            final ResultMessage resultMessage = messages.get(messageId, index);
            final Message message = new Message(resultMessage.getMessage());
            checkMessageReadPermission(message);

            return resultMessage;
        } catch (IndexMissingException e) {
            LOG.error("Index {} does not exist. Returning HTTP 404.", e.index().name());
            throw new NotFoundException(e);
        } catch (DocumentNotFoundException e) {
            LOG.error("Message {} does not exist in index {}. Returning HTTP 404.", messageId, index);
            throw new NotFoundException(e);
        }
    }

    private void checkMessageReadPermission(Message message) {
        boolean permitted = false;

        // if user has "admin" privileges, do not check stream permissions
        if (isPermitted(RestPermissions.STREAMS_READ, "*")) {
            return;
        }

        for (String streamId : message.getStreamIds()) {
            if (isPermitted(RestPermissions.STREAMS_READ, streamId)) {
                permitted = true;
                break;
            }
        }
        if (!permitted) {
            throw new ForbiddenException("Not authorized to access message " + message.getId());
        }
    }

    @GET
    @Path("/analyze")
    @Timed
    @ApiOperation(value = "Analyze a message string",
            notes = "Returns what tokens/terms a message string (message or full_message) is split to.")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(RestPermissions.MESSAGES_ANALYZE)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Specified index does not exist."),
    })
    public Map<String, List<String>> analyze(
            @ApiParam(name = "index", value = "The index the message containing the string is stored in.", required = true)
            @PathParam("index") String index,
            @ApiParam(name = "string", value = "The string to analyze.", required = true)
            @QueryParam("string") @NotEmpty String string) {
        final List<String> tokens;
        try {
            tokens = messages.analyze(string, index);
        } catch (IndexMissingException e) {
            LOG.error("Index does not exist. Returning HTTP 404.");
            throw new NotFoundException();
        }

        return ImmutableMap.of("tokens", tokens);
    }
}
