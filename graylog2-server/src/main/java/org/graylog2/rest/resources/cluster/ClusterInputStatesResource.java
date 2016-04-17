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
package org.graylog2.rest.resources.cluster;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.graylog2.rest.models.system.inputs.responses.InputStateSummary;
import org.graylog2.rest.models.system.inputs.responses.InputStatesList;
import org.graylog2.rest.resources.system.inputs.RemoteInputStatesResource;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RequiresAuthentication
@Api(value = "Cluster/InputState", description = "Cluster-wide input states")
@Path("/cluster/inputstates")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterInputStatesResource extends ProxiedResource {
    @Inject
    public ClusterInputStatesResource(NodeService nodeService,
                                      RemoteInterfaceProvider remoteInterfaceProvider,
                                      @Context HttpHeaders httpHeaders) throws NodeNotFoundException {
        super(httpHeaders, nodeService, remoteInterfaceProvider);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get all input states")
    @RequiresPermissions(RestPermissions.INPUTS_READ)
    public Map<String, Optional<Set<InputStateSummary>>> get() {
        return getForAllNodes(RemoteInputStatesResource::list, createRemoteInterfaceProvider(RemoteInputStatesResource.class), InputStatesList::states);
    }

    @PUT
    @Path("/{inputId}")
    @Timed
    @ApiOperation(value = "Start or restart specified input in all nodes")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input."),
    })
    public Map<String, Optional<InputCreated>> start(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) {
        return getForAllNodes(remoteResource -> remoteResource.start(inputId), createRemoteInterfaceProvider(RemoteInputStatesResource.class));
    }

    @DELETE
    @Path("/{inputId}")
    @Timed
    @ApiOperation(value = "Stop specified input in all nodes")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input."),
    })
    public Map<String, Optional<InputDeleted>> stop(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) {
        return getForAllNodes(remoteResource -> remoteResource.stop(inputId), createRemoteInterfaceProvider(RemoteInputStatesResource.class));
    }
}
