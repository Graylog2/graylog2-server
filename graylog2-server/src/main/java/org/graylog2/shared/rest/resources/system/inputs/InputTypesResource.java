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
import org.graylog2.Configuration;
import org.graylog2.rest.models.system.inputs.responses.InputTypeInfo;
import org.graylog2.rest.models.system.inputs.responses.InputTypesSummary;
import org.graylog2.shared.inputs.InputDescription;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiresAuthentication
@Api(value = "System/Inputs/Types", description = "Message input types of this node")
@Path("/system/inputs/types")
@Produces(MediaType.APPLICATION_JSON)
public class InputTypesResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(InputTypesResource.class);
    private final MessageInputFactory messageInputFactory;
    private final Configuration config;

    @Inject
    public InputTypesResource(MessageInputFactory messageInputFactory,
                              Configuration config) {
        this.messageInputFactory = messageInputFactory;
        this.config = config;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get all available input types of this node")
    public InputTypesSummary types() {
        Map<String, String> types = new HashMap<>();
        for (Map.Entry<String, InputDescription> entry : messageInputFactory.getAvailableInputs().entrySet()) {
            if (config.isCloud() && !entry.getValue().isCloudCompatible()) {
                continue;
            }
            types.put(entry.getKey(), entry.getValue().getName());
        }
        return InputTypesSummary.create(types);
    }

    @GET
    @Timed
    @Path("/all")
    @ApiOperation(value = "Get information about all input types")
    public Map<String, InputTypeInfo> all() {
        final Map<String, InputDescription> availableTypes = messageInputFactory.getAvailableInputs();
        Stream<Map.Entry<String, InputDescription>> inputStream = availableTypes
                .entrySet()
                .stream();
        if (config.isCloud()) {
            inputStream = inputStream.filter(e -> e.getValue().isCloudCompatible());
        }
        return inputStream.collect(Collectors.toMap(Map.Entry::getKey, entry -> {
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
            throwInputTypeNotFound(inputType);
        } else if (config.isCloud() && !description.isCloudCompatible()) {
            throwInputTypeNotFound(inputType);
        }

        return InputTypeInfo.create(inputType, description.getName(), description.isExclusive(), description.getRequestedConfiguration(), description.getLinkToDocs());
    }

    private static void throwInputTypeNotFound(String inputType) {
        final String message = "Unknown input type " + inputType + " requested.";
        LOG.error(message);
        throw new NotFoundException(message);
    }
}
