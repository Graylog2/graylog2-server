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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.requests.CreateStaticFieldRequest;
import org.graylog2.shared.inputs.PersistedInputs;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
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
    private PersistedInputs persistedInputs;

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
    @AuditEvent(type = AuditEventTypes.STATIC_FIELD_CREATE)
    public Response create(@ApiParam(name = "inputId", required = true)
                           @PathParam("inputId") String inputId,
                           @ApiParam(name = "JSON body", required = true)
                           @Valid @NotNull CreateStaticFieldRequest csfr) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.INPUTS_EDIT, inputId);

        final MessageInput input = persistedInputs.get(inputId);

        if (input == null) {
            final String msg = "Input <" + inputId + "> not found.";
            LOG.error(msg);
            throw new javax.ws.rs.NotFoundException(msg);
        }

        // Check if key is a valid message key.
        if (!Message.validKey(csfr.key())) {
            final String msg = "Invalid key: [" + csfr.key() + "]";
            LOG.error(msg);
            throw new BadRequestException(msg);
        }

        if (Message.RESERVED_FIELDS.contains(csfr.key()) && !Message.RESERVED_SETTABLE_FIELDS.contains(csfr.key())) {
            final String message = "Cannot add static field. Field [" + csfr.key() + "] is reserved.";
            LOG.error(message);
            throw new BadRequestException(message);
        }

        input.addStaticField(csfr.key(), csfr.value());

        final Input mongoInput = inputService.find(input.getPersistId());
        inputService.addStaticField(mongoInput, csfr.key(), csfr.value());

        final String msg = "Added static field [" + csfr.key() + "] to input <" + inputId + ">.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, StaticFieldsResource.class));

        final URI inputUri = getUriBuilderToSelf().path(InputsResource.class)
                .path("{inputId}")
                .build(mongoInput.getId());

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
    @AuditEvent(type = AuditEventTypes.STATIC_FIELD_DELETE)
    public void delete(@ApiParam(name = "Key", required = true)
                       @PathParam("key") String key,
                       @ApiParam(name = "inputId", required = true)
                       @PathParam("inputId") String inputId) throws NotFoundException {
        checkPermission(RestPermissions.INPUTS_EDIT, inputId);

        MessageInput input = persistedInputs.get(inputId);

        if (input == null) {
            final String msg = "Input <" + inputId + "> not found.";
            LOG.error(msg);
            throw new javax.ws.rs.NotFoundException(msg);
        }

        if (!input.getStaticFields().containsKey(key)) {
            final String msg = "No such static field [" + key + "] on input <" + inputId + ">.";
            LOG.error(msg);
            throw new javax.ws.rs.NotFoundException(msg);
        }

        input.getStaticFields().remove(key);

        Input mongoInput = inputService.find(input.getPersistId());
        inputService.removeStaticField(mongoInput, key);

        final String msg = "Removed static field [" + key + "] of input <" + inputId + ">.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, StaticFieldsResource.class));
    }
}
