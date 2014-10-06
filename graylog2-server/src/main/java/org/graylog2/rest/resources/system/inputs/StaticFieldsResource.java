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
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.Message;
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
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

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
    public Response create(@ApiParam(name = "inputId", required = true)
                           @PathParam("inputId") String inputId,
                           @ApiParam(name = "JSON body", required = true)
                           @Valid @NotNull CreateStaticFieldRequest csfr) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.INPUTS_EDIT, inputId);

        final MessageInput input = inputs.getRunningInput(inputId);

        if (input == null) {
            LOG.error("Input <{}> not found.", inputId);
            throw new javax.ws.rs.NotFoundException();
        }

        // Check if key is a valid message key.
        if (!Message.validKey(csfr.key)) {
            LOG.error("Invalid key: [{}]", csfr.key);
            throw new BadRequestException();
        }

        if (csfr.key == null || csfr.value == null || csfr.key.isEmpty() || csfr.value.isEmpty()) {
            LOG.error("Missing parameters. Returning HTTP 400.");
            throw new BadRequestException();
        }

        if (Message.RESERVED_FIELDS.contains(csfr.key) && !Message.RESERVED_SETTABLE_FIELDS.contains(csfr.key)) {
            LOG.error("Cannot add static field. Field [{}] is reserved.", csfr.key);
            throw new BadRequestException();
        }

        input.addStaticField(csfr.key, csfr.value);

        final Input mongoInput = inputService.find(input.getPersistId());
        inputService.addStaticField(mongoInput, csfr.key, csfr.value);

        final String msg = "Added static field [" + csfr.key + "] to input <" + inputId + ">.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, StaticFieldsResource.class));

        final URI inputUri = UriBuilder.fromResource(InputsResource.class)
                .path("{inputId}")
                .build(mongoInput.getInputId());

        return Response.created(inputUri).build();
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
    public void delete(@ApiParam(name = "Key", required = true)
                       @PathParam("key") String key,
                       @ApiParam(name = "inputId", required = true)
                       @PathParam("inputId") String inputId) throws NotFoundException {
        checkPermission(RestPermissions.INPUTS_EDIT, inputId);

        MessageInput input = inputs.getRunningInput(inputId);

        if (input == null) {
            LOG.error("Input <{}> not found.", inputId);
            throw new javax.ws.rs.NotFoundException();
        }

        if (!input.getStaticFields().containsKey(key)) {
            LOG.error("No such static field [{}] on input <{}>.", key, inputId);
            throw new javax.ws.rs.NotFoundException();
        }

        input.getStaticFields().remove(key);

        Input mongoInput = inputService.find(input.getPersistId());
        inputService.removeStaticField(mongoInput, key);

        final String msg = "Removed static field [" + key + "] of input <" + inputId + ">.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, StaticFieldsResource.class));
    }
}
