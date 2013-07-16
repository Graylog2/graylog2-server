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

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.sun.jersey.api.core.ResourceConfig;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.ValidationException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.Inputs;
import org.graylog2.inputs.NoSuchInputTypeException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MessageInputConfiguration;
import org.graylog2.plugin.inputs.MessageInputConfigurationException;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.inputs.requests.InputLaunchRequest;
import org.graylog2.streams.StreamImpl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/system/inputs")
public class InputsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(InputsResource.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Context
    ResourceConfig rc;

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(String body, @QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        InputLaunchRequest lr;
        try {
            lr = objectMapper.readValue(body, InputLaunchRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        // Build a proper configuration from POST data.
        MessageInputConfiguration inputConfig = new MessageInputConfiguration(lr.configuration);

        // Build input.
        MessageInput input = null;
        try {
            input = Inputs.factory(lr.type);
            input.configure(inputConfig, core);
        } catch (NoSuchInputTypeException e) {
            LOG.error("There is no such input type registered.", e);
            throw new WebApplicationException(e, Response.Status.NOT_FOUND);
        } catch (MessageInputConfigurationException e) {
            LOG.error("Missing or invalid input configuration.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        // Build MongoDB data and check if it would pass validation. We don't need to go on if it doesn't.
        Map<String, Object> inputData = Maps.newHashMap();
        inputData.put("title", lr.title);
        inputData.put("type", lr.type);
        inputData.put("creator_user_id", lr.creatorUserId);
        inputData.put("configuration", lr.configuration);
        inputData.put("created_at", new DateTime(DateTimeZone.UTC));

        Input mongoInput = new Input(core, inputData);
        if (!mongoInput.validate(inputData)) {
            LOG.error("Validation error.");
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
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("input_id", inputId);
        result.put("persist_id", id.toStringMongod());

        return Response.status(Response.Status.ACCEPTED).entity(json(result, prettyPrint)).build();
    }

}
