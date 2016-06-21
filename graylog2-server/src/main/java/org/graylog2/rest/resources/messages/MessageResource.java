/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.messages;

import com.codahale.metrics.annotation.Timed;
import com.eaio.uuid.UUID;
import com.google.common.net.InetAddresses;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.elasticsearch.index.IndexNotFoundException;
import org.graylog2.indexer.messages.DocumentNotFoundException;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.inputs.codecs.CodecsModule;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ResolvableInetSocketAddress;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.rest.models.messages.requests.MessageParseRequest;
import org.graylog2.rest.models.messages.responses.MessageTokens;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.net.InetSocketAddress;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

@RequiresAuthentication
@Api(value = "Messages", description = "Single messages")
@Produces(MediaType.APPLICATION_JSON)
@Path("/messages")
public class MessageResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(MessageResource.class);

    private Messages messages;
    private Map<String, Codec.Factory<? extends Codec>> codecFactory;

    @Inject
    public MessageResource(Messages messages, CodecsModule.CodecFactoryHolder codecFactoryHolder) {
        this.messages = messages;
        this.codecFactory = codecFactoryHolder.getFactory();
    }

    @GET
    @Path("/{index}/{messageId}")
    @Timed
    @ApiOperation(value = "Get a single message.")
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
            final Message message = resultMessage.getMessage();
            checkMessageReadPermission(message);

            return resultMessage;
        } catch (IndexNotFoundException e) {
            final String msg = "Index " + e.getIndex() + " does not exist.";
            LOG.error(msg, e);
            throw new NotFoundException(msg, e);
        } catch (DocumentNotFoundException e) {
            final String msg = "Message " + messageId + " does not exist in index " + index;
            LOG.error(msg, e);
            throw new NotFoundException(msg, e);
        }
    }

    private void checkMessageReadPermission(Message message) {
        // if user has "admin" privileges, do not check stream permissions
        if (isPermitted(RestPermissions.STREAMS_READ, "*")) {
            return;
        }

        boolean permitted = false;
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

    @POST
    @Path("/parse")
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Parse a raw message")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Specified codec does not exist.")
    })
    public ResultMessage parse(@ApiParam(name = "JSON body", required = true) MessageParseRequest request) {
        final Codec.Factory<? extends Codec> factory = codecFactory.get(request.codec());

        if (factory == null) {
            throw new NotFoundException("Codec " + request.codec() + " does not exist.");
        }

        final Configuration configuration = new Configuration(request.configuration());
        final Codec codec = factory.create(configuration);

        final ResolvableInetSocketAddress remoteAddress = ResolvableInetSocketAddress.wrap(new InetSocketAddress(request.remoteAddress(), 1234));

        final RawMessage rawMessage = new RawMessage(0, new UUID(), Tools.nowUTC(), remoteAddress, request.message().getBytes());
        final Message message = decodeMessage(codec, remoteAddress, rawMessage);

        return ResultMessage.createFromMessage(message);
    }

    private Message decodeMessage(Codec codec, ResolvableInetSocketAddress remoteAddress, RawMessage rawMessage) {
        final Message message = codec.decode(rawMessage);

        // Ensure the decoded Message has a source, otherwise creating a ResultMessage will fail
        if (isNullOrEmpty(message.getSource())) {
            final String address = InetAddresses.toAddrString(remoteAddress.getAddress());
            message.setSource(address);
        }

        // Override source
        if (codec.getConfiguration() != null && codec.getConfiguration().stringIsSet(Codec.Config.CK_OVERRIDE_SOURCE)) {
            message.setSource(codec.getConfiguration().getString(Codec.Config.CK_OVERRIDE_SOURCE));
        }

        return message;
    }

    @GET
    @Path("/{index}/analyze")
    @Timed
    @ApiOperation(value = "Analyze a message string",
            notes = "Returns what tokens/terms a message string (message or full_message) is split to.")
    @RequiresPermissions(RestPermissions.MESSAGES_ANALYZE)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Specified index does not exist."),
    })
    public MessageTokens analyze(
            @ApiParam(name = "index", value = "The index the message containing the string is stored in.", required = true)
            @PathParam("index") String index,
            @ApiParam(name = "string", value = "The string to analyze.", required = true)
            @QueryParam("string") @NotEmpty String string) {
        try {
            return MessageTokens.create(messages.analyze(string, index));
        } catch (IndexNotFoundException e) {
            final String message = "Index " + index + " does not exist.";
            LOG.error(message, e);
            throw new NotFoundException(message);
        }
    }
}
