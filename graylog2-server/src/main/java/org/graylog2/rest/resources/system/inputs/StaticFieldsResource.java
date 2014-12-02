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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.database.*;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.Message;
import com.wordnik.swagger.annotations.*;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.inputs.requests.CreateStaticFieldRequest;
import org.graylog2.security.RestPermissions;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Api(value = "StaticFields", description = "Static fields of an input")
@Path("/system/inputs/{inputId}/staticfields")
public class StaticFieldsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(StaticFieldsResource.class);

    @Inject
    private InputService inputService;
    @Inject
    private ActivityWriter activityWriter;
    @Inject
    private InputRegistry inputs;

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Add a static field to an input")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node."),
            @ApiResponse(code = 400, message = "Field/Key is reserved."),
            @ApiResponse(code = 400, message = "Missing or invalid configuration.")
    })
    public Response create(@ApiParam(name = "JSON body", required = true) String body,
                           @ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) throws NotFoundException {
        if (inputId == null || inputId.isEmpty()) {
            LOG.error("Missing inputId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
        checkPermission(RestPermissions.INPUTS_EDIT, inputId);

        MessageInput input = inputs.getRunningInput(inputId);

        if (input == null) {
            LOG.error("Input <{}> not found.", inputId);
            throw new WebApplicationException(404);
        }

        // Build extractor.
        CreateStaticFieldRequest csfr;
        try {
            csfr = objectMapper.readValue(body, CreateStaticFieldRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        // Check if key is a valid message key.
        if (!Message.validKey(csfr.key)) {
            LOG.error("Invalid key: [{}]", csfr.key);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if (csfr.key == null || csfr.value == null || csfr.key.isEmpty() || csfr.value.isEmpty()) {
            LOG.error("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if (Message.RESERVED_FIELDS.contains(csfr.key) && !Message.RESERVED_SETTABLE_FIELDS.contains(csfr.key)) {
            LOG.error("Cannot add static field. Field [{}] is reserved.", csfr.key);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        input.addStaticField(csfr.key, csfr.value);

        Input mongoInput = inputService.find(input.getPersistId());
        try {
            inputService.addStaticField(mongoInput, csfr.key, csfr.value);
        } catch (ValidationException e) {
            LOG.error("Static field persist validation failed.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        String msg = "Added static field [" + csfr.key + "] to input <" + inputId + ">.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, StaticFieldsResource.class));

        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Remove static field of an input")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node."),
            @ApiResponse(code = 404, message = "No such static field.")
    })
    @Path("/{key}")
    public Response delete(@ApiParam(name = "Key", required = true) @PathParam("key") String key,
                           @ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) throws NotFoundException {
        if (inputId == null || inputId.isEmpty()) {
            LOG.error("Missing inputId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
        checkPermission(RestPermissions.INPUTS_EDIT, inputId);

        MessageInput input = inputs.getRunningInput(inputId);

        if (input == null) {
            LOG.error("Input <{}> not found.", inputId);
            throw new WebApplicationException(404);
        }

        if(!input.getStaticFields().containsKey(key)) {
            LOG.error("No such static field [{}] on input <{}>.", key, inputId);
            throw new WebApplicationException(404);
        }

        input.getStaticFields().remove(key);

        Input mongoInput = inputService.find(input.getPersistId());
        inputService.removeStaticField(mongoInput, key);

        String msg = "Removed static field [" + key + "] of input <" + inputId + ">.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, StaticFieldsResource.class));

        return Response.status(Response.Status.NO_CONTENT).build();
    }


}
