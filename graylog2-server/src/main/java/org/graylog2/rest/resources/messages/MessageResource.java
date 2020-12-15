/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.messages.DocumentNotFoundException;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.inputs.codecs.CodecFactory;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.BadRequestException;
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
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

@RequiresAuthentication
@Api(value = "Messages", description = "Single messages")
@Produces(MediaType.APPLICATION_JSON)
@Path("/messages")
public class MessageResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(MessageResource.class);

    private final Messages messages;
    private final CodecFactory codecFactory;
    private final IndexSetRegistry indexSetRegistry;

    @Inject
    public MessageResource(Messages messages, CodecFactory codecFactory, IndexSetRegistry indexSetRegistry) {
        this.messages = requireNonNull(messages);
        this.codecFactory = requireNonNull(codecFactory);
        this.indexSetRegistry = requireNonNull(indexSetRegistry);
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
                                @PathParam("messageId") String messageId) throws IOException {
        checkPermission(RestPermissions.MESSAGES_READ, messageId);
        try {
            final ResultMessage resultMessage = messages.get(messageId, index);
            final Message message = resultMessage.getMessage();
            checkMessageReadPermission(message);

            return resultMessage;
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
            @ApiResponse(code = 404, message = "Specified codec does not exist."),
            @ApiResponse(code = 400, message = "Could not decode message.")
    })
    @NoAuditEvent("only used to parse a test message")
    public ResultMessage parse(@ApiParam(name = "JSON body", required = true) MessageParseRequest request) {
        Codec codec;
        try {
            final Configuration configuration = new Configuration(request.configuration());
            codec = codecFactory.create(request.codec(), configuration);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException(e);
        }

        final ResolvableInetSocketAddress remoteAddress = ResolvableInetSocketAddress.wrap(new InetSocketAddress(request.remoteAddress(), 1234));

        final RawMessage rawMessage = new RawMessage(0, new UUID(), Tools.nowUTC(), remoteAddress, request.message().getBytes(StandardCharsets.UTF_8));
        final Message message = decodeMessage(codec, remoteAddress, rawMessage);

        return ResultMessage.createFromMessage(message);
    }

    private Message decodeMessage(Codec codec, ResolvableInetSocketAddress remoteAddress, RawMessage rawMessage) {
        Message message;
        try {
            message = codec.decode(rawMessage);

        } catch (Exception e) {
            throw new BadRequestException("Could not decode message");
        }

        if (message == null) {
            throw new BadRequestException("Could not decode message");
        }

        // Ensure the decoded Message has a source, otherwise creating a ResultMessage will fail
        if (isNullOrEmpty(message.getSource())) {
            final String address = InetAddresses.toAddrString(remoteAddress.getAddress());
            message.setSource(address);
        }

        // Override source
        final Configuration configuration = codec.getConfiguration();
        if (configuration.stringIsSet(Codec.Config.CK_OVERRIDE_SOURCE)) {
            message.setSource(configuration.getString(Codec.Config.CK_OVERRIDE_SOURCE));
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
            @ApiParam(name = "analyzer", value = "The analyzer to use.")
            @QueryParam("analyzer") @Nullable String analyzer,
            @ApiParam(name = "string", value = "The string to analyze.", required = true)
            @QueryParam("string") @NotEmpty String string) throws IOException {

        final String indexAnalyzer = indexSetRegistry.getForIndex(index)
                .map(indexSet -> indexSet.getConfig().indexAnalyzer())
                .orElse("standard");
        final String messageAnalyzer = analyzer == null ? indexAnalyzer : analyzer;

        return MessageTokens.create(messages.analyze(string, index, messageAnalyzer));
    }
}
