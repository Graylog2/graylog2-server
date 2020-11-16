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
package org.graylog2.shared.rest.resources.system.inputs;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.rest.models.system.inputs.responses.InputTypeInfo;
import org.graylog2.rest.models.system.inputs.responses.InputTypesSummary;
import org.graylog2.shared.inputs.InputDescription;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "System/Inputs/Types", description = "Message input types of this node")
@Path("/system/inputs/types")
@Produces(MediaType.APPLICATION_JSON)
public class InputTypesResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(InputTypesResource.class);
    private final MessageInputFactory messageInputFactory;

    @Inject
    public InputTypesResource(MessageInputFactory messageInputFactory) {
        this.messageInputFactory = messageInputFactory;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get all available input types of this node")
    public InputTypesSummary types() {
        Map<String, String> types = new HashMap<>();
        for (Map.Entry<String, InputDescription> entry : messageInputFactory.getAvailableInputs().entrySet())
            types.put(entry.getKey(), entry.getValue().getName());
        return InputTypesSummary.create(types);
    }

    @GET
    @Timed
    @Path("/all")
    @ApiOperation(value = "Get information about all input types")
    public Map<String, InputTypeInfo> all() {
        final Map<String, InputDescription> availableTypes = messageInputFactory.getAvailableInputs();
        return availableTypes
                .entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> {
                    final InputDescription description = entry.getValue();
                    return InputTypeInfo.create(entry.getKey(), description.getName(), description.isExclusive(),
                            description.getRequestedConfiguration(), description.getLinkToDocs());
                }));
    }

    @GET
    @Timed
    @Path("{inputType}")
    @ApiOperation(value = "Get information about a single input type")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input type registered.")
    })
    public InputTypeInfo info(@ApiParam(name = "inputType", required = true) @PathParam("inputType") String inputType) {
        final InputDescription description = messageInputFactory.getAvailableInputs().get(inputType);
        if (description == null) {
            final String message = "Unknown input type " + inputType + " requested.";
            LOG.error(message);
            throw new NotFoundException(message);
        }

        return InputTypeInfo.create(inputType, description.getName(), description.isExclusive(), description.getRequestedConfiguration(), description.getLinkToDocs());
    }
}
