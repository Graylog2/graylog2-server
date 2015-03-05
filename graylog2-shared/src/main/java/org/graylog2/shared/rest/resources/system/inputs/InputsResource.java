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
package org.graylog2.shared.rest.resources.system.inputs;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableSet;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.responses.InputSummary;
import org.graylog2.shared.inputs.PersistedInputs;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputStateSummary;
import org.graylog2.rest.models.system.inputs.responses.InputsList;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.inputs.InputLauncher;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.rest.models.system.inputs.requests.InputLaunchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

@RequiresAuthentication
@Api(value = "System/Inputs", description = "Message inputs of this node")
@Path("/system/inputs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InputsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(InputsResource.class);

    private final InputRegistry inputRegistry;
    private final MessageInputFactory messageInputFactory;
    private final InputLauncher inputLauncher;
    private final PersistedInputs persistedInputs;

    @Inject
    public InputsResource(InputRegistry inputRegistry,
                          MessageInputFactory messageInputFactory,
                          InputLauncher inputLauncher,
                          PersistedInputs persistedInputs) {
        this.inputRegistry = inputRegistry;
        this.messageInputFactory = messageInputFactory;
        this.inputLauncher = inputLauncher;
        this.persistedInputs = persistedInputs;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get information of a single input on this node")
    @Path("/{inputId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node.")
    })
    public InputSummary single(@ApiParam(name = "inputId", required = true)
                                      @PathParam("inputId") String inputId) {
        checkPermission(RestPermissions.INPUTS_READ, inputId);

        final MessageInput input = inputRegistry.getRunningInput(inputId);
        if (input == null) {
            LOG.info("Input [{}] not found. Returning HTTP 404.", inputId);
            throw new NotFoundException();
        }

        return InputSummary.create(input.getTitle(),
                input.getPersistId(),
                input.isGlobal(),
                input.getName(),
                input.getContentPack(),
                input.getId(),
                input.getCreatedAt(),
                input.getClass().getCanonicalName(),
                input.getCreatorUserId(),
                input.getAttributesWithMaskedPasswords(),
                input.getStaticFields()
        );
    }

    @GET
    @Timed
    @ApiOperation(value = "Get all inputs of this node")
    public InputsList list() {
        final ImmutableSet.Builder<InputStateSummary> inputStates = ImmutableSet.builder();
        for (IOState<MessageInput> inputState : inputRegistry.getInputStates()) {
            if (!isPermitted(RestPermissions.INPUTS_READ, inputState.getStoppable().getId()))
                continue;
            inputStates.add(getInputStateSummary(inputState));
        }

        return InputsList.create(inputStates.build());
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
                        messageInput.getPersistId(),
                        messageInput.isGlobal(),
                        messageInput.getName(),
                        messageInput.getContentPack(),
                        messageInput.getId(),
                        messageInput.getCreatedAt(),
                        messageInput.getClass().getCanonicalName(),
                        messageInput.getCreatorUserId(),
                        messageInput.getAttributesWithMaskedPasswords(),
                        messageInput.getStaticFields()
                )
        );
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
    public Response create(@ApiParam(name = "JSON body", required = true)
                           @Valid @NotNull InputLaunchRequest lr) throws ValidationException {
        restrictToMaster();
        checkPermission(RestPermissions.INPUTS_CREATE);

        // Build input.
        final MessageInput input;
        try {
            final String nodeId;
            if (lr.node() != null)
                nodeId = lr.node();
            else
                nodeId = serverStatus.getNodeId().toString();

            input = messageInputFactory.create(lr, getCurrentUser().getName(), nodeId);

            input.checkConfiguration();
        } catch (NoSuchInputTypeException e) {
            LOG.error("There is no such input type registered.", e);
            throw new NotFoundException(e);
        } catch (ConfigurationException e) {
            LOG.error("Missing or invalid input configuration.", e);
            throw new BadRequestException(e);
        }

        // Don't run if exclusive and another instance is already running.
        if (input.isExclusive() && inputRegistry.hasTypeRunning(input.getClass())) {
            final String error = "Type is exclusive and already has input running.";
            LOG.error(error);
            throw new BadRequestException(error);
        }

        if (!persistedInputs.add(input))
            throw new RuntimeException("Unable to persist input.");

        if (input.isGlobal() || input.getNodeId().equals(serverStatus.getNodeId().toString())) {
            input.initialize();

            // Launch input. (this will run async and clean up itself in case of an error.)
            inputLauncher.launch(input);
        }

        final URI inputUri = getUriBuilderToSelf().path(InputsResource.class)
                .path("{inputId}")
                .build(input.getId());

        return Response.created(inputUri).entity(InputCreated.create(input.getId())).build();
    }

    @DELETE
    @Timed
    @Path("/{inputId}")
    @ApiOperation(value = "Terminate input on this node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node.")
    })
    public void terminate(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) {
        checkPermission(RestPermissions.INPUTS_TERMINATE, inputId);

        MessageInput messageInput = inputRegistry.getRunningInput(inputId);

        if (messageInput == null) {
            LOG.info("Cannot terminate input. Input not found.");
            throw new NotFoundException();
        }

        inputRegistry.remove(messageInput);

        if (serverStatus.hasCapability(ServerStatus.Capability.MASTER) || !messageInput.isGlobal()) {
            persistedInputs.remove(messageInput);
        }
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
    public Response update(@ApiParam(name = "JSON body", required = true) @Valid @NotNull InputLaunchRequest lr,
                           @ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) {
        checkPermission(RestPermissions.INPUTS_EDIT, inputId);

        final MessageInput oldInput = persistedInputs.get(inputId);
        final MessageInput messageInput;
        try {
            messageInput = messageInputFactory.create(lr, getCurrentUser().getName(), oldInput.getNodeId());
            persistedInputs.update(inputId, messageInput);
        } catch (NoSuchInputTypeException e) {
            LOG.error("Unable to construct MessageInput: ", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final URI inputUri = getUriBuilderToSelf().path(InputsResource.class)
                .path("{inputId}")
                .build(inputId);

        return Response.created(inputUri).entity(InputCreated.create(inputId)).build();
    }

    @POST
    @Timed
    @Path("/{inputId}/launch")
    @ApiOperation(value = "Launch existing input on this node")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node.")
    })
    public void launchExisting(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) {
        final IOState<MessageInput> inputState = inputRegistry.getInputState(inputId);
        final MessageInput messageInput;

        if (inputState == null) {
            messageInput = persistedInputs.get(inputId);
            messageInput.initialize();
        } else
            messageInput = inputState.getStoppable();

        if (messageInput == null) {
            final String error = "Cannot launch input <" + inputId + ">. Input not found.";
            LOG.info(error);
            throw new NotFoundException(error);
        }

        inputLauncher.launch(messageInput);
    }

    @POST
    @Timed
    @Path("/{inputId}/stop")
    @ApiOperation(value = "Stop existing input on this node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node.")
    })
    public InputStateSummary stop(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) {
        final MessageInput input = inputRegistry.getRunningInput(inputId);
        if (input == null) {
            LOG.info("Cannot stop input. Input not found.");
            throw new NotFoundException();
        }

        final IOState<MessageInput> inputState = inputRegistry.stop(input);

        return getInputStateSummary(inputState);
    }

    @POST
    @Timed
    @Path("/{inputId}/restart")
    @ApiOperation(value = "Restart existing input on this node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node.")
    })
    public Response restart(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) {
        final IOState<MessageInput> oldState = inputRegistry.getRunningInputState(inputId);
        stop(inputId);
        inputRegistry.remove(oldState);

        launchExisting(inputId);
        return Response.status(Response.Status.ACCEPTED).build();
    }
}
