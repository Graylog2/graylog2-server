/*
 * Copyright 2012-2014 TORCH GmbH
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
 */
package org.graylog2.radio.rest.resources.system.inputs;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.inputs.InputState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.radio.rest.resources.RestResource;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.shared.rest.resources.system.inputs.requests.InputLaunchRequest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/system/inputs")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InputsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(InputsResource.class);
    
    @Inject
    private InputRegistry inputRegistry;

    @GET @Timed
    public String list() {
        List<Map<String, Object>> inputStates = Lists.newArrayList();

        for (InputState inputState : inputRegistry.getInputStates()) {
            inputStates.add(inputState.asMap());
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("inputs", inputStates);
        result.put("total", inputStates.size());

        return json(result);
    }

    @GET @Timed
    @Path("/{inputId}")
    public String single(@PathParam("inputId") String inputId) {
        MessageInput input = inputRegistry.getRunningInput(inputId);

        if (input == null) {
            LOG.info("Input [{}] not found. Returning HTTP 404.", inputId);
            throw new WebApplicationException(404);
        }

        return json(input.asMap());

    }

    @POST @Timed
    public Response launch(String body) {

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
        MessageInput input;
        try {
            input = inputRegistry.create(lr.type);
            input.setTitle(lr.title);
            input.setCreatorUserId(lr.creatorUserId);
            input.setCreatedAt(createdAt);
            input.setGlobal(lr.global);

            input.checkConfiguration(inputConfig);
        } catch (NoSuchInputTypeException e) {
            LOG.error("There is no such input type registered.", e);
            throw new WebApplicationException(e, Response.Status.NOT_FOUND);
        } catch (ConfigurationException e) {
            LOG.error("Missing or invalid input configuration.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        String inputId = UUID.randomUUID().toString();
        //input.setPersistId(inputId);

        // Don't run if exclusive and another instance is already running.
        if (input.isExclusive() && inputRegistry.hasTypeRunning(input.getClass())) {
            LOG.error("Type is exclusive and already has input running.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        input.initialize(inputConfig);

        // Launch input. (this will run async and clean up itself in case of an error.)
        inputRegistry.launch(input, inputId, true);

        Map<String, Object> result = Maps.newHashMap();
        result.put("input_id", inputId);
        result.put("persist_id", inputId);

        return Response.status(Response.Status.ACCEPTED).entity(json(result)).build();
    }


    @DELETE @Timed
    @Path("/{inputId}")
    public Response terminate(@PathParam("inputId") String inputId) {
        MessageInput input = inputRegistry.getRunningInput(inputId);

        if (input == null) {
            LOG.info("Cannot terminate input. Input not found.");
            throw new WebApplicationException(404);
        }

        String msg = "Attempting to terminate input [" + input.getName()+ "]. Reason: REST request.";
        LOG.info(msg);

        inputRegistry.terminate(input);

        String msg2 = "Terminated input [" + input.getName()+ "]. Reason: REST request.";
        LOG.info(msg2);

        return Response.status(Response.Status.ACCEPTED).build();
    }

    @GET @Timed
    @Path("/types")
    public String types() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("types", inputRegistry.getAvailableInputs());

        return json(result);
    }

    @GET @Timed
    @Path("/types/{inputType}")
    public String info(@PathParam("inputType") String inputType) {

        MessageInput input;
        try {
            input = inputRegistry.create(inputType);
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

    @POST @Timed
    @Path("/{inputId}/launch")
    public Response launchExisting(@PathParam("inputId") String inputId) {
        InputState inputState = inputRegistry.getInputState(inputId);

        if (inputState == null) {
            throw new NotFoundException("Input <" + inputId + "> not found!");
        }

        final MessageInput input = inputState.getMessageInput();

        if (input == null) {
            final String error = "Cannot launch input <" + inputId + ">. Input not found.";
            LOG.info(error);
            throw new NotFoundException(error);
        }

        String msg = "Launching existing input [" + input.getName()+ "]. Reason: REST request.";
        LOG.info(msg);

        inputRegistry.launch(inputState);

        String msg2 = "Launched existing input [" + input.getName()+ "]. Reason: REST request.";
        LOG.info(msg2);

        Map<String, Object> result = Maps.newHashMap();
        result.put("input_id", inputId);
        result.put("persist_id", inputId);

        return Response.status(Response.Status.ACCEPTED).entity(json(result)).build();
    }

    @POST @Timed
    @Path("/{inputId}/stop")
    public Response stop(@PathParam("inputId") String inputId) {
        final MessageInput input = inputRegistry.getRunningInput(inputId);
        if (input == null) {
            LOG.info("Cannot stop input. Input not found.");
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        String msg = "Stopping input [" + input.getName()+ "]. Reason: REST request.";
        LOG.info(msg);

        inputRegistry.stop(input);

        String msg2 = "Stopped input [" + input.getName()+ "]. Reason: REST request.";
        LOG.info(msg2);

        return Response.status(Response.Status.ACCEPTED).build();
    }

    @POST @Timed
    @Path("/{inputId}/restart")
    public Response restart(@PathParam("inputId") String inputId) {
        stop(inputId);
        launchExisting(inputId);
        return Response.status(Response.Status.ACCEPTED).build();
    }
}
