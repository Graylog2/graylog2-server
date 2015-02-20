/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.system.outputs;

import autovalue.shaded.com.google.common.common.collect.Sets;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.database.NotFoundException;
import org.graylog2.outputs.MessageOutputFactory;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.rest.helpers.OutputFilter;
import org.graylog2.rest.models.system.outputs.responses.OutputSummary;
import org.graylog2.rest.resources.streams.outputs.AvailableOutputSummary;
import org.graylog2.rest.resources.streams.outputs.OutputListResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.outputs.CreateOutputRequest;
import org.graylog2.utilities.ConfigurationMapConverter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RequiresAuthentication
@Api(value = "System/Outputs", description = "Manage outputs")
@Path("/system/outputs")
public class OutputResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(OutputResource.class);

    private final OutputService outputService;
    private final MessageOutputFactory messageOutputFactory;
    private final OutputFilter outputFilter;

    @Inject
    public OutputResource(OutputService outputService,
                          MessageOutputFactory messageOutputFactory,
                          OutputFilter outputFilter) {
        this.outputService = outputService;
        this.messageOutputFactory = messageOutputFactory;
        this.outputFilter = outputFilter;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all outputs")
    @RequiresPermissions(RestPermissions.STREAM_OUTPUTS_CREATE)
    @Produces(MediaType.APPLICATION_JSON)
    public OutputListResponse get() {
        checkPermission(RestPermissions.OUTPUTS_READ);
        final Set<OutputSummary> outputs = new HashSet<>();

        for (Output output : outputService.loadAll())
            outputs.add(OutputSummary.create(
                    output.getId(),
                    output.getTitle(),
                    output.getType(),
                    output.getCreatorUserId(),
                    new DateTime(output.getCreatedAt()),
                    new HashMap<>(output.getConfiguration()),
                    output.getContentPack()
            ));

        return OutputListResponse.create(outputFilter.filterPasswordFields(outputs));
    }

    @GET
    @Path("/{outputId}")
    @Timed
    @ApiOperation(value = "Get specific output")
    @RequiresPermissions(RestPermissions.OUTPUTS_CREATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such output on this node.")
    })
    public OutputSummary get(@ApiParam(name = "outputId", value = "The id of the output we want.", required = true) @PathParam("outputId") String outputId) throws NotFoundException {
        checkPermission(RestPermissions.OUTPUTS_READ, outputId);
        try {
            final Output output = outputService.load(outputId);
            return outputFilter.filterPasswordFields(
                    OutputSummary.create(output.getId(), output.getTitle(), output.getType(), output.getCreatorUserId(), new DateTime(output.getCreatedAt()), output.getConfiguration(), output.getContentPack())
            );
        } catch (MessageOutputConfigurationException e) {
            LOG.error("Unable to filter configuration fields of output {}: ", outputId, e);
            return null;
        }
    }

    @POST
    @Timed
    @ApiOperation(value = "Create an output")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid output specification in input.", response = OutputSummary.class)
    })
    public Response create(@ApiParam(name = "JSON body", required = true) CreateOutputRequest csor) throws ValidationException {
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
        final URI outputUri = UriBuilder.fromResource(OutputResource.class)
                .path("{outputId}")
                .build(output.getId());

        try {
            return Response.created(outputUri).entity(
                    outputFilter.filterPasswordFields(
                            OutputSummary.create(
                                    output.getId(),
                                    output.getTitle(),
                                    output.getType(),
                                    output.getCreatorUserId(),
                                    new DateTime(output.getCreatedAt()),
                                    new HashMap<>(output.getConfiguration()),
                                    output.getContentPack()
                            )
                    )
            ).build();
        } catch (MessageOutputConfigurationException e) {
            LOG.error("Unable to filter configuration fields for output {}: ", output.getId(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{outputId}")
    @Timed
    @ApiOperation(value = "Delete output")
    @RequiresPermissions(RestPermissions.OUTPUTS_TERMINATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such stream/output on this node.")
    })
    public void delete(@ApiParam(name = "outputId", value = "The id of the output that should be deleted", required = true)
                       @PathParam("outputId") String outputId) throws org.graylog2.database.NotFoundException {
        checkPermission(RestPermissions.OUTPUTS_TERMINATE);
        Output output = outputService.load(outputId);
        outputService.destroy(output);
    }

    @GET
    @Path("/available")
    @Timed
    @ApiOperation(value = "Get all available output modules")
    @RequiresPermissions(RestPermissions.STREAMS_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, AvailableOutputSummary>> available() {
        return ImmutableMap.of("types", messageOutputFactory.getAvailableOutputs());
    }

}
