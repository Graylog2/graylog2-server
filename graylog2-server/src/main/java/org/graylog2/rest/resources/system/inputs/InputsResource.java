/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.rest.resources.system.inputs;

import com.beust.jcommander.internal.Lists;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.bson.types.ObjectId;
import org.graylog2.database.ValidationException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputRegistry;
import org.graylog2.plugin.inputs.InputState;
import org.graylog2.inputs.NoSuchInputTypeException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.inputs.requests.InputLaunchRequest;
import org.graylog2.security.RestPermissions;
import org.graylog2.system.activities.Activity;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Api(value = "System/Inputs", description = "Message inputs of this node")
@Path("/system/inputs")
public class InputsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(InputsResource.class);

    @GET @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get information of a single input on this node")
    @Path("/{inputId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node.")
    })
    public String single(@ApiParam(title = "inputId", required = true) @PathParam("inputId") String inputId) {
        checkPermission(RestPermissions.INPUTS_READ, inputId);

        MessageInput input = core.inputs().getRunningInput(inputId);

        if (input == null) {
            LOG.info("Input [{}] not found. Returning HTTP 404.", inputId);
            throw new WebApplicationException(404);
        }

        return json(input.asMap());

    }

    @GET @Timed
    @ApiOperation(value = "Get all inputs of this node")
    @Produces(MediaType.APPLICATION_JSON)
    public String list() {
        List<Map<String, Object>> inputStates = Lists.newArrayList();
        Map<String, Object> result = Maps.newHashMap();
        for (InputState inputState : core.inputs().getInputStates()) {
			checkPermission(RestPermissions.INPUTS_READ, inputState.getMessageInput().getId());
            inputStates.add(inputState.asMap());
		}
        result.put("inputs", inputStates);
        result.put("total", inputStates.size());

        return json(result);
    }

    @POST @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Launch input on this node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input type registered"),
            @ApiResponse(code = 400, message = "Missing or invalid configuration"),
            @ApiResponse(code = 400, message = "Type is exclusive and already has input running")
    })
    public Response create(@ApiParam(title = "JSON body", required = true) String body) {
        checkPermission(RestPermissions.INPUTS_CREATE);

        InputLaunchRequest lr;
        try {
            lr = objectMapper.readValue(body, InputLaunchRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        // Build a proper configuration from POST data.
        Configuration inputConfig = new Configuration(lr.configuration);

        // Build input.
        DateTime createdAt = new DateTime(DateTimeZone.UTC);
        MessageInput input = null;
        try {
            input = InputRegistry.factory(lr.type);
            input.initialize(inputConfig, core);
            input.setTitle(lr.title);
            input.setGlobal(lr.global);
            input.setCreatorUserId(lr.creatorUserId);
            input.setCreatedAt(createdAt);

            input.checkConfiguration();
        } catch (NoSuchInputTypeException e) {
            LOG.error("There is no such input type registered.", e);
            throw new WebApplicationException(e, Response.Status.NOT_FOUND);
        } catch (ConfigurationException e) {
            LOG.error("Missing or invalid input configuration.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        String inputId = UUID.randomUUID().toString();

        // Build MongoDB data
        Map<String, Object> inputData = Maps.newHashMap();
        inputData.put("input_id", inputId);
        inputData.put("title", lr.title);
        inputData.put("type", lr.type);
        inputData.put("creator_user_id", lr.creatorUserId);
        inputData.put("configuration", lr.configuration);
        inputData.put("created_at", createdAt);
        if (lr.global)
            inputData.put("global", true);
        else
            inputData.put("node_id", core.getNodeId());

        // ... and check if it would pass validation. We don't need to go on if it doesn't.
        Input mongoInput = new Input(core, inputData);
        if (!mongoInput.validate(inputData)) {
            LOG.error("Validation error.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        // Don't run if exclusive and another instance is already running.
        if (input.isExclusive() && core.inputs().hasTypeRunning(input.getClass())) {
            LOG.error("Type is exclusive and already has input running.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        // Persist input.
        ObjectId id;
        try {
            id = mongoInput.save();
            input.setPersistId(id.toStringMongod());
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        // Launch input. (this will run async and clean up itself in case of an error.)
        core.inputs().launch(input, inputId);

        Map<String, Object> result = Maps.newHashMap();
        result.put("input_id", inputId);
        result.put("persist_id", id.toStringMongod());

        return Response.status(Response.Status.ACCEPTED).entity(json(result)).build();
    }

    @GET @Timed
    @Path("/types")
    @ApiOperation(value = "Get all available input types of this node")
    @Produces(MediaType.APPLICATION_JSON)
    public String types() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("types", core.inputs().getAvailableInputs());

        return json(result);
    }

    @DELETE @Timed
    @Path("/{inputId}")
    @ApiOperation(value = "Terminate input on this node")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node.")
    })
    public Response terminate(@ApiParam(title = "inputId", required = true) @PathParam("inputId") String inputId) {
        checkPermission(RestPermissions.INPUTS_TERMINATE, inputId);

        MessageInput input = core.inputs().getRunningInput(inputId);

        if (input == null) {
            LOG.info("Cannot terminate input. Input not found.");
            throw new WebApplicationException(404);
        }

        String msg = "Attempting to terminate input [" + input.getName()+ "]. Reason: REST request.";
        LOG.info(msg);
        core.getActivityWriter().write(new Activity(msg, InputsResource.class));

        // Shutdown actual input.
        input.stop();

        if (core.isMaster() || !input.getGlobal()) {
            // Remove from list and mongo.
            core.inputs().cleanInput(input);
        }
        core.inputs().removeFromRunning(input);

        String msg2 = "Terminated input [" + input.getName()+ "]. Reason: REST request.";
        LOG.info(msg2);
        core.getActivityWriter().write(new Activity(msg2, InputsResource.class));

        return Response.status(Response.Status.ACCEPTED).build();
    }

    @GET @Timed
    @Path("/{inputId}/launch")
    @ApiOperation(value = "Launch existing input on this node")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node.")
    })
    public Response launchExisting(@ApiParam(title = "inputId", required = true) @PathParam("inputId") String inputId) {
        MessageInput input = null;
        try {
             input = InputRegistry.getMessageInput(Input.findForThisNode(core, inputId), core);
        } catch (NoSuchInputTypeException e) {
            LOG.info("Cannot launch input. Input not found.");
            throw new WebApplicationException(404);
        } catch (ConfigurationException e) {
            LOG.info("Cannot launch input. Configuration is invalid.");
            throw new WebApplicationException(404);
        }

        if (input == null) {
            LOG.info("Cannot launch input. Input not found.");
            throw new WebApplicationException(404);
        }

        String msg = "Launching existing input [" + input.getName()+ "]. Reason: REST request.";
        LOG.info(msg);
        core.getActivityWriter().write(new Activity(msg, InputsResource.class));

        core.inputs().launch(input);

        String msg2 = "Launched existing input [" + input.getName()+ "]. Reason: REST request.";
        LOG.info(msg2);
        core.getActivityWriter().write(new Activity(msg2, InputsResource.class));

        return Response.status(Response.Status.ACCEPTED).build();
    }

    @GET @Timed
    @Path("/types/{inputType}")
    @ApiOperation(value = "Get information about a single input type")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input type registered.")
    })
    public String info(@ApiParam(title = "inputType", required = true) @PathParam("inputType") String inputType) {

        MessageInput input;
        try {
            input = InputRegistry.factory(inputType);
        } catch (NoSuchInputTypeException e) {
            LOG.error("There is no such input type registered.", e);
            throw new WebApplicationException(e, Response.Status.NOT_FOUND);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("type", input.getClass().getCanonicalName());
        result.put("name", input.getName());
        result.put("is_exclusive", input.isExclusive());
        result.put("requested_configuration", input.getRequestedConfiguration().asList());
        result.put("link_to_docs", input.linkToDocs());

        return json(result);
    }

}
