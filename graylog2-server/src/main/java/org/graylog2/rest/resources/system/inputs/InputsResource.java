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
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.ValidationException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.Inputs;
import org.graylog2.inputs.NoSuchInputTypeException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.inputs.requests.InputLaunchRequest;
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
import java.util.Set;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/system/inputs")
public class InputsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(InputsResource.class);

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String list() {
        List<Map<String, Object>> inputs = Lists.newArrayList();

        for (MessageInput input : core.inputs().getRunningInputs().values()) {
            Map<String, Object> inputMap = Maps.newHashMap();

            inputMap.put("input_id", input.getId());
            inputMap.put("persist_id", input.getPersistId());
            inputMap.put("name", input.getName());
            inputMap.put("title", input.getTitle());
            inputMap.put("creator_user_id", input.getCreatorUserId());
            inputMap.put("started_at", Tools.getISO8601String(input.getCreatedAt()));
            inputMap.put("attributes", input.getAttributes());

            inputs.add(inputMap);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("inputs", inputs);
        result.put("total", core.inputs().runningCount());

        return json(result);
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(String body) {
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
            input = Inputs.factory(lr.type);
            input.configure(inputConfig, core);
            input.setTitle(lr.title);
            input.setCreatorUserId(lr.creatorUserId);
            input.setCreatedAt(createdAt);
        } catch (NoSuchInputTypeException e) {
            LOG.error("There is no such input type registered.", e);
            throw new WebApplicationException(e, Response.Status.NOT_FOUND);
        } catch (ConfigurationException e) {
            LOG.error("Missing or invalid input configuration.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        // Build MongoDB data
        Map<String, Object> inputData = Maps.newHashMap();
        inputData.put("title", lr.title);
        inputData.put("type", lr.type);
        inputData.put("creator_user_id", lr.creatorUserId);
        inputData.put("configuration", lr.configuration);
        inputData.put("created_at", createdAt);

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

        // Launch input.
        String inputId;
        try {
            inputId = core.inputs().launch(input);
        } catch (Exception e) {
            LOG.error("Could not launch new input.", e);
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
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

        Map<String, Object> result = Maps.newHashMap();
        result.put("input_id", inputId);
        result.put("persist_id", id.toStringMongod());

        return Response.status(Response.Status.ACCEPTED).entity(json(result)).build();
    }

    @GET
    @Timed
    @Path("/types")
    @Produces(MediaType.APPLICATION_JSON)
    public String types() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("types", core.inputs().getAvailableInputs());

        return json(result);
    }

    @DELETE
    @Timed
    @Path("/{inputId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response terminate(@PathParam("inputId") String inputId) {
        MessageInput input = core.inputs().getRunningInputs().get(inputId);

        String msg = "Attempting to terminate input [" + input.getName()+ "]. Reason: REST request.";
        LOG.info(msg);
        core.getActivityWriter().write(new Activity(msg, InputsResource.class));

        if (input == null) {
            LOG.info("Cannot terminate input. Input not found.");
            throw new WebApplicationException(404);
        }

        // Delete in Mongo.
        Input.destroy(new BasicDBObject("_id", new ObjectId(input.getPersistId())), core, Input.COLLECTION);

        // Shutdown actual input.
        input.stop();
        core.inputs().getRunningInputs().remove(input.getId());

        String msg2 = "Terminated input [" + input.getName()+ "]. Reason: REST request.";
        LOG.info(msg2);
        core.getActivityWriter().write(new Activity(msg2, InputsResource.class));

        return Response.status(Response.Status.ACCEPTED).build();
    }

    @GET
    @Timed
    @Path("/types/{inputType}")
    @Produces(MediaType.APPLICATION_JSON)
    public String info(@PathParam("inputType") String inputType) {

        MessageInput input;
        try {
            input = Inputs.factory(inputType);
        } catch (NoSuchInputTypeException e) {
            LOG.error("There is no such input type registered.", e);
            throw new WebApplicationException(e, Response.Status.NOT_FOUND);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("type", input.getClass().getCanonicalName());
        result.put("name", input.getName());
        result.put("is_exclusive", input.isExclusive());
        result.put("requested_configuration", input.getRequestedConfiguration().asList());

        return json(result);
    }


}
