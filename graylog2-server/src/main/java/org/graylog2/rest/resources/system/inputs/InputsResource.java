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
package org.graylog2.rest.resources.system.inputs;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.Configuration;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.inputs.encryption.EncryptedInputConfigs;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputSummary;
import org.graylog2.rest.models.system.inputs.responses.InputsList;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "System/Inputs", description = "Message inputs", tags = {CLOUD_VISIBLE})
@Path("/system/inputs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InputsResource extends AbstractInputsResource {

    private static final Logger LOG = LoggerFactory.getLogger(InputsResource.class);

    private final InputService inputService;
    private final MessageInputFactory messageInputFactory;
    private final Configuration config;

    @Inject
    public InputsResource(InputService inputService, MessageInputFactory messageInputFactory, Configuration config) {
        super(messageInputFactory.getAvailableInputs());
        this.inputService = inputService;
        this.messageInputFactory = messageInputFactory;
        this.config = config;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get information of a single input on this node")
    @Path("/{inputId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input.")
    })
    public InputSummary get(@ApiParam(name = "inputId", required = true)
                            @PathParam("inputId") String inputId) throws org.graylog2.database.NotFoundException {
        checkPermission(RestPermissions.INPUTS_READ, inputId);

        final Input input = inputService.find(inputId);

        return getInputSummary(input);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get all inputs")
    public InputsList list() {
        final Set<InputSummary> inputs = inputService.all().stream()
                .filter(input -> isPermitted(RestPermissions.INPUTS_READ, input.getId()))
                .map(this::getInputSummary)
                .collect(Collectors.toSet());

        return InputsList.create(inputs);
    }

    @POST
    @Timed
    @ApiOperation(
            value = "Launch input on this node",
            response = InputCreated.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input type registered"),
            @ApiResponse(code = 400, message = "Missing or invalid configuration"),
            @ApiResponse(code = 400, message = "Type is exclusive and already has input running")
    })
    @RequiresPermissions(RestPermissions.INPUTS_CREATE)
    @AuditEvent(type = AuditEventTypes.MESSAGE_INPUT_CREATE)
    public Response create(@ApiParam(name = "JSON body", required = true)
                           @Valid @NotNull InputCreateRequest lr) throws ValidationException {
        try {
            throwBadRequestIfNotGlobal(lr);
            // TODO Configuration type values need to be checked. See ConfigurationMapConverter.convertValues()
            final MessageInput messageInput = messageInputFactory.create(lr, getCurrentUser().getName(), lr.node());
            if (config.isCloud() && !messageInput.isCloudCompatible()) {
                throw new BadRequestException(String.format(Locale.ENGLISH,
                        "The input type <%s> is not allowed in the cloud environment!", lr.type()));
            }

            messageInput.checkConfiguration();
            final Input input = this.inputService.create(messageInput.asMap());
            final String newId = inputService.save(input);
            final URI inputUri = getUriBuilderToSelf().path(InputsResource.class)
                    .path("{inputId}")
                    .build(newId);

            return Response.created(inputUri).entity(InputCreated.create(newId)).build();
        } catch (NoSuchInputTypeException e) {
            LOG.error("There is no such input type registered.", e);
            throw new NotFoundException("There is no such input type registered.", e);
        } catch (ConfigurationException e) {
            LOG.error("Missing or invalid input configuration.", e);
            throw new BadRequestException("Missing or invalid input configuration.", e);
        }

    }

    @DELETE
    @Timed
    @Path("/{inputId}")
    @ApiOperation(value = "Terminate input on this node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node.")
    })
    @AuditEvent(type = AuditEventTypes.MESSAGE_INPUT_DELETE)
    public void terminate(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) throws org.graylog2.database.NotFoundException {
        checkPermission(RestPermissions.INPUTS_TERMINATE, inputId);
        final Input input = inputService.find(inputId);
        inputService.destroy(input);
    }

    @PUT
    @Timed
    @Path("/{inputId}")
    @ApiOperation(
            value = "Update input on this node",
            response = InputCreated.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node."),
            @ApiResponse(code = 400, message = "Missing or invalid input configuration.")
    })
    @AuditEvent(type = AuditEventTypes.MESSAGE_INPUT_UPDATE)
    public Response update(@ApiParam(name = "JSON body", required = true) @Valid @NotNull InputCreateRequest lr,
                           @ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) throws org.graylog2.database.NotFoundException, NoSuchInputTypeException, ConfigurationException, ValidationException {

        throwBadRequestIfNotGlobal(lr);
        checkPermission(RestPermissions.INPUTS_EDIT, inputId);

        final Input input = inputService.find(inputId);

        final MessageInput messageInput = messageInputFactory.create(lr, getCurrentUser().getName(), lr.node());

        messageInput.checkConfiguration();

        final Map<String, Object> mergedInput = new HashMap<>(input.getFields());
        mergedInput.putAll(messageInput.asMap());

        // Special handling for encrypted values
        final Map<String, Object> origConfig = input.getConfiguration();
        final Map<String, Object> updatedConfig = Objects.requireNonNullElse(messageInput.getConfiguration().getSource(), Map.of());
        mergedInput.put(MessageInput.FIELD_CONFIGURATION, EncryptedInputConfigs.merge(origConfig, updatedConfig));

        final Input newInput = inputService.create(input.getId(), mergedInput);
        inputService.update(newInput);

        final URI inputUri = getUriBuilderToSelf().path(InputsResource.class)
                .path("{inputId}")
                .build(input.getId());

        return Response.created(inputUri).entity(InputCreated.create(input.getId())).build();
    }

    private void throwBadRequestIfNotGlobal(InputCreateRequest lr) {
        if (config.isCloud() && !lr.global()) {
            throw new BadRequestException("Only global inputs are allowed in the cloud environment!");
        }
    }
}
