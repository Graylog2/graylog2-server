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
package org.graylog2.rest.resources.streams.outputs;

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
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.outputs.AddOutputRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Set;

@RequiresAuthentication
@Api(value = "StreamOutputs", description = "Manage stream outputs for a given stream")
@Path("/streams/{streamid}/outputs")
public class StreamOutputResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(StreamOutputResource.class);

    private final OutputService outputService;
    private final StreamService streamService;

    @Inject
    public StreamOutputResource(OutputService outputService, StreamService streamService) {
        this.outputService = outputService;
        this.streamService = streamService;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all outputs for a stream")
    @RequiresPermissions(RestPermissions.STREAM_OUTPUTS_CREATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such stream on this node.")
    })
    public Map<String, Object> get(@ApiParam(name = "streamid", value = "The id of the stream whose outputs we want.", required = true)
                                   @PathParam("streamid") String streamid) throws org.graylog2.database.NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamid);
        checkPermission(RestPermissions.STREAM_OUTPUTS_READ);

        final Stream stream = streamService.load(streamid);
        final Set<Output> outputs = stream.getOutputs();

        return ImmutableMap.of(
                "outputs", outputs,
                "total", outputs.size());
    }

    @GET
    @Path("/{outputId}")
    @Timed
    @ApiOperation(value = "Get specific output of a stream")
    @RequiresPermissions(RestPermissions.STREAM_OUTPUTS_READ)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such stream/output on this node.")
    })
    public Output get(@ApiParam(name = "streamid", value = "The id of the stream whose outputs we want.", required = true) @PathParam("streamid") String streamid,
                      @ApiParam(name = "outputId", value = "The id of the output we want.", required = true) @PathParam("outputId") String outputId) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamid);
        checkPermission(RestPermissions.STREAM_OUTPUTS_READ, outputId);

        return outputService.load(outputId);
    }

    @POST
    @Timed
    @ApiOperation(value = "Associate outputs with a stream")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid output specification in input.")
    })
    public Response add(@ApiParam(name = "streamid", value = "The id of the stream whose outputs we want.", required = true)
                        @PathParam("streamid") String streamid,
                        @ApiParam(name = "JSON body", required = true)
                        @Valid @NotNull AddOutputRequest request) throws ValidationException, NotFoundException {
        checkPermission(RestPermissions.STREAM_OUTPUTS_CREATE);

        final Stream stream = streamService.load(streamid);
        for (String outputId : request.outputs) {
            final Output output = outputService.load(outputId);
            streamService.addOutput(stream, output);
        }

        return Response.accepted().build();
    }

    @DELETE
    @Path("/{outputId}")
    @Timed
    @ApiOperation(value = "Delete output of a stream")
    @RequiresPermissions(RestPermissions.STREAM_OUTPUTS_DELETE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such stream/output on this node.")
    })
    public void remove(@ApiParam(name = "streamid", value = "The id of the stream whose outputs we want.", required = true)
                       @PathParam("streamid") String streamid,
                       @ApiParam(name = "outputId", value = "The id of the output that should be deleted", required = true)
                       @PathParam("outputId") String outputId) throws org.graylog2.database.NotFoundException {
        checkPermission(RestPermissions.STREAM_OUTPUTS_DELETE);

        final Stream stream = streamService.load(streamid);
        final Output output = outputService.load(outputId);

        streamService.removeOutput(stream, output);
    }
}
