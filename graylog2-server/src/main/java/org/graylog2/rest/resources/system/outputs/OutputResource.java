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
package org.graylog2.rest.resources.system.outputs;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.outputs.MessageOutputFactory;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.rest.models.streams.outputs.OutputListResponse;
import org.graylog2.rest.models.streams.outputs.requests.CreateOutputRequest;
import org.graylog2.rest.models.system.outputs.responses.OutputSummary;
import org.graylog2.rest.resources.streams.outputs.AvailableOutputSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.OutputService;
import org.graylog2.utilities.ConfigurationMapConverter;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RequiresAuthentication
@Tag(name = "System/Outputs", description = "Manage outputs")
@Path("/system/outputs")
public class OutputResource extends RestResource {
    private final OutputService outputService;
    private final MessageOutputFactory messageOutputFactory;

    @Inject
    public OutputResource(OutputService outputService,
                          MessageOutputFactory messageOutputFactory) {
        this.outputService = outputService;
        this.messageOutputFactory = messageOutputFactory;
    }

    @GET
    @Timed
    @Operation(summary = "Get a list of all outputs")
    @Produces(MediaType.APPLICATION_JSON)
    public OutputListResponse get() {
        checkPermission(RestPermissions.OUTPUTS_READ);
        final Set<OutputSummary> outputs = new HashSet<>();

        for (Output output : outputService.loadAll()) {
            outputs.add(OutputSummary.create(
                    output.getId(),
                    output.getTitle(),
                    output.getType(),
                    output.getCreatorUserId(),
                    new DateTime(output.getCreatedAt()),
                    new HashMap<>(output.getConfiguration()),
                    output.getContentPack()
            ));
        }

        return OutputListResponse.create(outputs);
    }

    @GET
    @Path("/{outputId}")
    @Timed
    @Operation(summary = "Get specific output")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "No such output on this node.")
    })
    public OutputSummary get(@Parameter(name = "outputId", description = "The id of the output we want.", required = true) @PathParam("outputId") String outputId) throws NotFoundException {
        checkPermission(RestPermissions.OUTPUTS_READ, outputId);
        final Output output = outputService.load(outputId);
        return OutputSummary.create(output.getId(), output.getTitle(), output.getType(), output.getCreatorUserId(), new DateTime(output.getCreatedAt()), output.getConfiguration(), output.getContentPack());
    }

    @POST
    @Timed
    @Operation(summary = "Create an output")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Output created successfully",
                    content = @Content(schema = @Schema(implementation = OutputSummary.class))),
            @ApiResponse(responseCode = "400", description = "Invalid output specification in input.")
    })
    @AuditEvent(type = AuditEventTypes.MESSAGE_OUTPUT_CREATE)
    public Response create(@RequestBody(required = true) CreateOutputRequest csor) throws ValidationException {
        checkPermission(RestPermissions.OUTPUTS_CREATE);
        final AvailableOutputSummary outputSummary = messageOutputFactory.getAvailableOutputs().get(csor.type());

        if (outputSummary == null) {
            throw new ValidationException("type", "Invalid output type");
        }

        // Make sure the config values will be stored with the correct type.
        final CreateOutputRequest createOutputRequest = CreateOutputRequest.create(
                csor.title(),
                csor.type(),
                ConfigurationMapConverter.convertValues(csor.configuration(), outputSummary.requestedConfiguration()),
                csor.streams()
        );

        final Output output = outputService.create(createOutputRequest, getCurrentUser().getName());
        final URI outputUri = getUriBuilderToSelf().path(OutputResource.class)
                .path("{outputId}")
                .build(output.getId());

        return Response.created(outputUri).entity(
                OutputSummary.create(
                        output.getId(),
                        output.getTitle(),
                        output.getType(),
                        output.getCreatorUserId(),
                        new DateTime(output.getCreatedAt()),
                        new HashMap<>(output.getConfiguration()),
                        output.getContentPack()
                )
        ).build();
    }

    @DELETE
    @Path("/{outputId}")
    @Timed
    @Operation(summary = "Delete output")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Success"),
            @ApiResponse(responseCode = "404", description = "No such stream/output on this node.")
    })
    @AuditEvent(type = AuditEventTypes.MESSAGE_OUTPUT_DELETE)
    public void delete(@Parameter(name = "outputId", description = "The id of the output that should be deleted", required = true)
                       @PathParam("outputId") String outputId) throws org.graylog2.database.NotFoundException {
        checkPermission(RestPermissions.OUTPUTS_TERMINATE);
        final Output output = outputService.load(outputId);
        outputService.destroy(output);
    }

    @GET
    @Path("/available")
    @Timed
    @Operation(summary = "Get all available output modules")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, AvailableOutputSummary>> available() {
        checkPermission(RestPermissions.OUTPUTS_READ);
        return ImmutableMap.of("types", messageOutputFactory.getAvailableOutputs());
    }

    @PUT
    @Path("/{outputId}")
    @Timed
    @Operation(summary = "Update output")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "No such output on this node.")
    })
    @AuditEvent(type = AuditEventTypes.MESSAGE_OUTPUT_UPDATE)
    public Output update(@Parameter(name = "outputId", description = "The id of the output that should be deleted", required = true)
                         @PathParam("outputId") String outputId,
                         @RequestBody(required = true) Map<String, Object> deltas) throws ValidationException, NotFoundException {
        checkPermission(RestPermissions.OUTPUTS_EDIT, outputId);
        final Output oldOutput = outputService.load(outputId);
        final AvailableOutputSummary outputSummary = messageOutputFactory.getAvailableOutputs().get(oldOutput.getType());

        if (outputSummary == null) {
            throw new ValidationException("type", "Invalid output type");
        }

        deltas.remove("streams");
        if (deltas.containsKey("configuration")) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> configuration = (Map<String, Object>) deltas.get("configuration");
            deltas.put("configuration", ConfigurationMapConverter.convertValues(configuration, outputSummary.requestedConfiguration()));
        }

        return this.outputService.update(outputId, deltas);
    }
}
