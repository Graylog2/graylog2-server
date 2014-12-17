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
package org.graylog2.rest.resources.system.inputs;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.database.ValidationException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputImpl;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.inputs.responses.InputCreated;
import org.graylog2.rest.resources.system.inputs.responses.InputStateSummary;
import org.graylog2.rest.resources.system.inputs.responses.InputSummary;
import org.graylog2.rest.resources.system.inputs.responses.InputsList;
import org.graylog2.security.RestPermissions;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.shared.rest.resources.system.inputs.requests.InputLaunchRequest;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RequiresAuthentication
@Api(value = "System/Inputs", description = "Message inputs of this node")
@Path("/system/inputs")
public class InputsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(InputsResource.class);

    private final InputService inputService;
    private final InputRegistry inputRegistry;
    private final ActivityWriter activityWriter;
    private final MessageInputFactory messageInputFactory;

    @Inject
    public InputsResource(InputService inputService, InputRegistry inputRegistry, ActivityWriter activityWriter, MessageInputFactory messageInputFactory) {
        this.inputService = inputService;
        this.inputRegistry = inputRegistry;
        this.activityWriter = activityWriter;
        this.messageInputFactory = messageInputFactory;
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
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
    @Produces(MediaType.APPLICATION_JSON)
    public InputsList list() {
        final ImmutableSet.Builder<InputStateSummary> inputStates = ImmutableSet.builder();
        for (IOState<MessageInput> inputState : inputRegistry.getInputStates()) {
            if (!isPermitted(RestPermissions.INPUTS_READ, inputState.getStoppable().getId()))
                continue;
            final MessageInput messageInput = inputState.getStoppable();
            inputStates.add(InputStateSummary.create(
                    inputState.getId(),
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
            ));
        }

        return InputsList.create(inputStates.build());
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
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
        checkPermission(RestPermissions.INPUTS_CREATE);

        // Build a proper configuration from POST data.
        final Configuration inputConfig = new Configuration(lr.configuration());

        // Build input.
        final MessageInput input;
        try {
            input = messageInputFactory.create(lr, getCurrentUser().getName());

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

        final String inputId = UUID.randomUUID().toString();
        final Input mongoInput = getInput(input);
        // Persist input.
        String id = inputService.save(mongoInput);
        input.setPersistId(id);

        input.initialize();

        // Launch input. (this will run async and clean up itself in case of an error.)
        inputRegistry.launch(input, inputId);

        final URI inputUri = UriBuilder.fromResource(InputsResource.class)
                .path("{inputId}")
                .build(inputId);

        return Response.created(inputUri).entity(InputCreated.create(inputId, id)).build();
    }

    private Input getInput(MessageInput input) throws ValidationException {
        // Build MongoDB data
        final Map<String, Object> inputData = Maps.newHashMap();
        inputData.put(MessageInput.FIELD_INPUT_ID, input.getId());
        inputData.put(MessageInput.FIELD_TITLE, input.getTitle());
        inputData.put(MessageInput.FIELD_TYPE, input.getType());
        inputData.put(MessageInput.FIELD_CREATOR_USER_ID, getCurrentUser().getName());
        inputData.put(MessageInput.FIELD_CONFIGURATION, input.getConfiguration().getSource());
        inputData.put(MessageInput.FIELD_CREATED_AT, Tools.iso8601());

        if (input.isGlobal()) {
            inputData.put(MessageInput.FIELD_GLOBAL, true);
        } else {
            inputData.put(MessageInput.FIELD_NODE_ID, serverStatus.getNodeId().toString());
        }

        // ... and check if it would pass validation. We don't need to go on if it doesn't.
        final Input mongoInput = new InputImpl(inputData);

        return mongoInput;
    }

    @DELETE
    @Timed
    @Path("/{inputId}")
    @ApiOperation(value = "Terminate input on this node")
    @Produces(MediaType.APPLICATION_JSON)
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

        final String msg = "Attempting to terminate input [" + messageInput.getName() + "]. Reason: REST request.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, InputsResource.class));

        inputRegistry.terminate(messageInput);

        if (serverStatus.hasCapability(ServerStatus.Capability.MASTER) || !messageInput.isGlobal()) {
            // Remove from list and mongo.
            try {
                final Input input = inputService.find(messageInput.getId());
                inputService.destroy(input);
            } catch (org.graylog2.database.NotFoundException e) {
                LOG.warn("Input not found while deleting it: ", e);
            }
        }

        final String msg2 = "Terminated input [" + messageInput.getName() + "]. Reason: REST request.";
        LOG.info(msg2);
        activityWriter.write(new Activity(msg2, InputsResource.class));
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
            try {
                final Input input = inputService.find(inputId);
                messageInput = inputService.getMessageInput(input);
                messageInput.initialize();
            } catch (NoSuchInputTypeException | org.graylog2.database.NotFoundException e) {
                final String error = "Cannot launch input <" + inputId + ">. Input not found.";
                LOG.info(error);
                throw new NotFoundException(error);
            }
        } else
            messageInput = inputState.getStoppable();

        if (messageInput == null) {
            final String error = "Cannot launch input <" + inputId + ">. Input not found.";
            LOG.info(error);
            throw new NotFoundException(error);
        }

        final String msg = "Launching existing input [" + messageInput.getName() + "]. Reason: REST request.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, InputsResource.class));

        if (inputState == null) {
            inputRegistry.launchPersisted(messageInput);
        } else {
            inputRegistry.launch(inputState);
        }

        final String msg2 = "Launched existing input [" + messageInput.getName() + "]. Reason: REST request.";
        LOG.info(msg2);
        activityWriter.write(new Activity(msg2, InputsResource.class));
    }

    @POST
    @Timed
    @Path("/{inputId}/stop")
    @ApiOperation(value = "Stop existing input on this node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node.")
    })
    public void stop(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) {
        final MessageInput input = inputRegistry.getRunningInput(inputId);
        if (input == null) {
            LOG.info("Cannot stop input. Input not found.");
            throw new NotFoundException();
        }

        final String msg = "Stopping input [" + input.getName() + "]. Reason: REST request.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, InputsResource.class));

        inputRegistry.stop(input);

        final String msg2 = "Stopped input [" + input.getName() + "]. Reason: REST request.";
        LOG.info(msg2);
        activityWriter.write(new Activity(msg2, InputsResource.class));
    }

    @POST
    @Timed
    @Path("/{inputId}/restart")
    @ApiOperation(value = "Restart existing input on this node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node.")
    })
    public Response restart(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) {
        stop(inputId);
        launchExisting(inputId);
        return Response.status(Response.Status.ACCEPTED).build();
    }
}
