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
import com.google.common.eventbus.EventBus;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.graylog2.rest.models.system.inputs.responses.InputSetup;
import org.graylog2.rest.models.system.inputs.responses.InputStateSummary;
import org.graylog2.rest.models.system.inputs.responses.InputStatesList;
import org.graylog2.rest.models.system.inputs.responses.InputSummary;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "System/InputStates", description = "Message input states of this node")
@Path("/system/inputstates")
@Produces(MediaType.APPLICATION_JSON)
public class InputStatesResource extends AbstractInputsResource {
    private static final Logger LOG = LoggerFactory.getLogger(InputStatesResource.class);
    private final InputRegistry inputRegistry;
    private final EventBus serverEventBus;
    private final InputService inputService;

    @Inject
    public InputStatesResource(InputRegistry inputRegistry,
                               EventBus serverEventBus,
                               InputService inputService,
                               MessageInputFactory messageInputFactory) {
        super(messageInputFactory.getAvailableInputs());
        this.inputRegistry = inputRegistry;
        this.serverEventBus = serverEventBus;
        this.inputService = inputService;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get all input states of this node")
    public InputStatesList list() {
        final Set<InputStateSummary> result = this.inputRegistry.stream()
                .filter(inputState -> isPermitted(RestPermissions.INPUTS_READ, inputState.getStoppable().getId()))
                .map(this::getInputStateSummary)
                .collect(Collectors.toSet());

        return InputStatesList.create(result);
    }

    @GET
    @Path("/{inputId}")
    @Timed
    @ApiOperation(value = "Get input state for specified input id on this node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node."),
    })
    public InputStateSummary get(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) {
        checkPermission(RestPermissions.INPUTS_READ, inputId);
        final IOState<MessageInput> inputState = this.inputRegistry.getInputState(inputId);
        if (inputState == null) {
            throw new NotFoundException("No input state for input id <" + inputId + "> on this node.");
        }
        return getInputStateSummary(inputState);
    }

    @PUT
    @Path("/{inputId}")
    @Timed
    @ApiOperation(value = "(Re-)Start specified input on this node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node."),
    })
    @AuditEvent(type = AuditEventTypes.MESSAGE_INPUT_START)
    public InputCreated start(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) throws org.graylog2.database.NotFoundException {
        checkPermission(RestPermissions.INPUTS_CHANGESTATE, inputId);
        final Input input = inputService.find(inputId);
        inputService.persistDesiredState(input, IOState.Type.RUNNING);
        final InputCreated result = InputCreated.create(inputId);
        this.serverEventBus.post(result);

        return result;
    }

    @PUT
    @Path("/setup/{inputId}")
    @Timed
    @ApiOperation(value = "Switch specified input to setup mode")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node."),
    })
    @AuditEvent(type = AuditEventTypes.MESSAGE_INPUT_SETUP)
    public InputSetup setup(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) throws org.graylog2.database.NotFoundException {
        checkPermission(RestPermissions.INPUTS_CHANGESTATE, inputId);
        final Input input = inputService.find(inputId);
        inputService.persistDesiredState(input, IOState.Type.SETUP);
        final InputSetup result = InputSetup.create(inputId);
        this.serverEventBus.post(result);

        return result;
    }

    @DELETE
    @Path("/{inputId}")
    @Timed
    @ApiOperation(value = "Stop specified input on this node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node."),
    })
    @AuditEvent(type = AuditEventTypes.MESSAGE_INPUT_STOP)
    public InputDeleted stop(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) throws org.graylog2.database.NotFoundException {
        checkPermission(RestPermissions.INPUTS_CHANGESTATE, inputId);
        final Input input = inputService.find(inputId);
        inputService.persistDesiredState(input, IOState.Type.STOPPED);
        final InputDeleted result = InputDeleted.create(inputId);
        this.serverEventBus.post(result);

        return result;
    }

    private InputStateSummary getInputStateSummary(IOState<MessageInput> inputState) {
        final MessageInput messageInput = inputState.getStoppable();
        return InputStateSummary.create(
                messageInput.getId(),
                inputState.getState().toString(),
                inputState.getStartedAt(),
                inputState.getDetailedMessage(),
                InputSummary.create(
                        messageInput.getTitle(),
                        messageInput.isGlobal(),
                        messageInput.getName(),
                        messageInput.getContentPack(),
                        messageInput.getId(),
                        messageInput.getCreatedAt(),
                        messageInput.getType(),
                        messageInput.getCreatorUserId(),
                        // Ensure password masking!
                        maskPasswordsInConfiguration(
                                messageInput.getConfiguration().getSource(),
                                messageInput.getRequestedConfiguration()
                        ),
                        messageInput.getStaticFields(),
                        messageInput.getNodeId()
                )
        );
    }
}
