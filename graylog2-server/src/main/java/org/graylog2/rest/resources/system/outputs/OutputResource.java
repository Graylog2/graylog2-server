package org.graylog2.rest.resources.system.outputs;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.graylog2.outputs.MessageOutputFactory;
import org.graylog2.plugin.streams.Output;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.streams.outputs.AvailableOutputSummary;
import org.graylog2.security.RestPermissions;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.outputs.CreateOutputRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@RequiresAuthentication
@Api(value = "Outputs", description = "Manage outputs")
@Path("/system/outputs")
public class OutputResource extends RestResource {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private final OutputService outputService;
    private final MessageOutputFactory messageOutputFactory;

    @Inject
    public OutputResource(OutputService outputService, MessageOutputFactory messageOutputFactory, ObjectMapper objectMapper1) {
        this.outputService = outputService;
        this.messageOutputFactory = messageOutputFactory;
        System.out.println("Constructor ObjectMapper: " + objectMapper1);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all outputs")
    @RequiresPermissions(RestPermissions.STREAM_OUTPUTS_CREATE)
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {
        checkPermission(RestPermissions.OUTPUTS_READ);
        return Response.status(Response.Status.OK).entity(outputService.loadAll()).build();
    }

    @GET @Path("/{outputId}")
    @Timed
    @ApiOperation(value = "Get specific output")
    @RequiresPermissions(RestPermissions.OUTPUTS_CREATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such output on this node.")
    })
    public Output get(@ApiParam(title = "outputId", description = "The id of the output we want.", required = true) @PathParam("outputId") String outputId) throws NotFoundException {
        checkPermission(RestPermissions.OUTPUTS_READ, outputId);
        return outputService.load(outputId);
    }

    @POST
    @Timed
    @ApiOperation(value = "Create an output")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid output specification in input.")
    })
    public Response create(@ApiParam(title = "JSON body", required = true) CreateOutputRequest csor) throws ValidationException {
        checkPermission(RestPermissions.OUTPUTS_CREATE);

        Output output = outputService.create(csor);

        return Response.status(Response.Status.CREATED).entity(output).build();
    }

    @DELETE @Path("/{outputId}")
    @Timed
    @ApiOperation(value = "Delete output")
    @RequiresPermissions(RestPermissions.OUTPUTS_TERMINATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such stream/output on this node.")
    })
    public Response delete(@ApiParam(title = "outputId", description = "The id of the output that should be deleted", required = true) @PathParam("outputId") String outputId) throws org.graylog2.database.NotFoundException {
        Output output = outputService.load(outputId);
        outputService.destroy(output);

        return Response.status(Response.Status.OK).build();
    }

    @GET @Path("/available")
    @Timed
    @ApiOperation(value = "Get all available output modules")
    @RequiresPermissions(RestPermissions.STREAMS_READ)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such stream on this node.")
    })
    public Map<String, AvailableOutputSummary> available(@ApiParam(title = "streamid", description = "The stream id for which available outputs should be listed", required = true) @PathParam("streamid") String streamId) {
        return messageOutputFactory.getAvailableOutputs();
    }
}
