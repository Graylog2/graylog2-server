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
package org.graylog2.radio.rest.resources.system.inputs;

import com.beust.jcommander.internal.Lists;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.radio.inputs.InputRegistry;
import org.graylog2.radio.inputs.NoSuchInputTypeException;
import org.graylog2.radio.rest.resources.RestResource;
import org.graylog2.radio.rest.resources.system.inputs.requests.InputLaunchRequest;
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
import java.util.concurrent.ExecutionException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/system/inputs")
public class InputsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(InputsResource.class);

    @GET @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String list() {
        List<Map<String, Object>> inputs = Lists.newArrayList();

        for (MessageInput input : radio.inputs().getRunningInputs().values()) {
            inputs.add(input.asMap());
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("inputs", inputs);
        result.put("total", radio.inputs().runningCount());

        return json(result);
    }

    @GET @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{inputId}")
    public String single(@PathParam("inputId") String inputId) {
        MessageInput input = radio.inputs().getRunningInputs().get(inputId);

        if (input == null) {
            LOG.info("Input [{}] not found. Returning HTTP 404.", inputId);
            throw new WebApplicationException(404);
        }

        return json(input.asMap());

    }

    @POST @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
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
            input = InputRegistry.factory(lr.type);
            input.initialize(inputConfig, radio);
            input.setTitle(lr.title);
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

        // Don't run if exclusive and another instance is already running.
        if (input.isExclusive() && radio.inputs().hasTypeRunning(input.getClass())) {
            LOG.error("Type is exclusive and already has input running.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        // Launch input. (this will run async and clean up itself in case of an error.)
        radio.inputs().launch(input, inputId, true);

        return Response.status(Response.Status.ACCEPTED).build();
    }


    @DELETE @Timed
    @Path("/{inputId}")
    public Response terminate(@PathParam("inputId") String inputId) {
        MessageInput input = radio.inputs().getRunningInputs().get(inputId);

        String msg = "Attempting to terminate input [" + input.getName()+ "]. Reason: REST request.";
        LOG.info(msg);

        if (input == null) {
            LOG.info("Cannot terminate input. Input not found.");
            throw new WebApplicationException(404);
        }

        // Unregister on server cluster.
        try {
            radio.inputs().unregisterInCluster(input);
        } catch (Exception e) {
            LOG.error("Could not unregister input [" + input.getName()+ "] on server cluster. Not continuing.", e);
            return Response.status(Response.Status.BAD_GATEWAY).build();
        }

        LOG.info("Unregistered input [" + input.getName()+ "] on server cluster.");

        // Shutdown actual input.
        input.stop();
        radio.inputs().getRunningInputs().remove(input.getId());

        String msg2 = "Terminated input [" + input.getName()+ "]. Reason: REST request.";
        LOG.info(msg2);

        return Response.status(Response.Status.ACCEPTED).build();
    }

    @GET @Timed
    @Path("/types")
    @Produces(MediaType.APPLICATION_JSON)
    public String types() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("types", radio.inputs().getAvailableInputs());

        return json(result);
    }

    @GET @Timed
    @Path("/types/{inputType}")
    @Produces(MediaType.APPLICATION_JSON)
    public String info(@PathParam("inputType") String inputType) {

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
