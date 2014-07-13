package org.graylog2.rest.resources.streams.outputs;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.database.*;
import org.graylog2.outputs.MessageOutputFactory;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamOutput;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.graylog2.streams.StreamOutputImpl;
import org.graylog2.streams.StreamOutputService;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.outputs.CreateStreamOutputRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@RequiresAuthentication
@Api(value = "StreamOutputs", description = "Manage stream outputs for a given stream")
@Path("/streams/{streamid}/outputs")
public class StreamOutputResource extends RestResource {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private final StreamOutputService streamOutputService;
    private final StreamService streamService;
    private final MessageOutputFactory messageOutputFactory;

    @Inject
    public StreamOutputResource(StreamOutputService streamOutputService, StreamService streamService, MessageOutputFactory messageOutputFactory) {
        this.streamOutputService = streamOutputService;
        this.streamService = streamService;
        this.messageOutputFactory = messageOutputFactory;
    }

    @GET @Timed
    @ApiOperation(value = "Get a list of all outputs for a stream")
    @RequiresPermissions(RestPermissions.STREAM_OUTPUTS_CREATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such stream on this node.")
    })
    public Set<StreamOutput> get(@ApiParam(title = "streamid", description = "The id of the stream whose outputs we want.", required = true) @PathParam("streamid") String streamid) {
        checkPermission(RestPermissions.STREAMS_READ, streamid);
        checkPermission(RestPermissions.STREAM_OUTPUTS_READ);
        final Stream stream;
        try {
            stream = streamService.load(streamid);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new javax.ws.rs.NotFoundException("Stream not found!");
        }

        return streamOutputService.loadAllForStream(stream);
    }

    @GET @Path("/{outputId}")
    @Timed
    @ApiOperation(value = "Get specific output of a stream")
    @RequiresPermissions(RestPermissions.STREAM_OUTPUTS_CREATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such stream/output on this node.")
    })
    public StreamOutput get(@ApiParam(title = "streamid", description = "The id of the stream whose outputs we want.", required = true) @PathParam("streamid") String streamid,
                            @ApiParam(title = "outputId", description = "The id of the output we want.", required = true) @PathParam("outputId") String outputId) {
        checkPermission(RestPermissions.STREAMS_READ, streamid);
        checkPermission(RestPermissions.STREAM_OUTPUTS_READ, outputId);

        try {
            streamService.load(streamid);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new javax.ws.rs.NotFoundException("Stream not found!");
        }

        try {
            return streamOutputService.load(outputId);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new javax.ws.rs.NotFoundException("Stream output not found!");
        }
    }

    @POST @Timed
    @ApiOperation(value = "Create an alarm callback")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such stream on this node.")
    })
    public Response create(@ApiParam(title = "streamid", description = "The stream id this new output belongs to.", required = true) @PathParam("streamid") String streamid,
                           @ApiParam(title = "JSON body", required = true) CreateStreamOutputRequest csor) {
        checkPermission(RestPermissions.STREAM_OUTPUTS_CREATE);
        try {
            streamService.load(streamid);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new javax.ws.rs.NotFoundException("Stream not found!");
        }


        Map<String, Object> result = Maps.newHashMap();

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

    @DELETE @Path("/{outputId}")
    @Timed
    @ApiOperation(value = "Delete output of a stream")
    @RequiresPermissions(RestPermissions.STREAM_OUTPUTS_DELETE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such stream/output on this node.")
    })
    public Response delete(@ApiParam(title = "streamid", description = "The stream id the output belongs to", required = true) @PathParam("streamid") String streamId,
                           @ApiParam(title = "outputId", description = "The id of the output that should be deleted", required = true) @PathParam("outputId") String outputId) {

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

    @POST @Path("/test")
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public StreamOutput create(@ApiParam(title = "streamid", description = "The stream id this new output belongs to.", required = true) @PathParam("streamid") String streamid,
                           @ApiParam(title = "JSON body", required = true) StreamOutputImpl streamOutput) {
        return streamOutput;
    }
}
