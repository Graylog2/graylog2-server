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
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.loggers.responses.LoggersSummary;
import org.graylog2.rest.models.system.loggers.responses.SubsystemSummary;
import org.graylog2.rest.resources.system.logs.RemoteLoggersResource;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.hibernate.validator.constraints.NotEmpty;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RequiresAuthentication
@Api(value = "Cluster/System/Loggers", description = "Cluster-wide access to internal Graylog loggers")
@Path("/cluster/system/loggers")
public class ClusterLoggersResource extends ProxiedResource {
    @Inject
    public ClusterLoggersResource(NodeService nodeService,
                                    RemoteInterfaceProvider remoteInterfaceProvider,
                                    @Context HttpHeaders httpHeaders) throws NodeNotFoundException {
        super(httpHeaders, nodeService, remoteInterfaceProvider);
    }

    @GET
    @Timed
    @ApiOperation(value = "List all loggers of all nodes and their current levels")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Optional<LoggersSummary>> loggers() {
        return getForAllNodes(RemoteLoggersResource::loggers, createRemoteInterfaceProvider(RemoteLoggersResource.class));
    }

    @GET
    @Timed
    @Path("/subsystems")
    @ApiOperation(value = "List all logger subsystems and their current levels")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Optional<SubsystemSummary>> subsystems() {
        return getForAllNodes(RemoteLoggersResource::subsystems, createRemoteInterfaceProvider(RemoteLoggersResource.class));
    }

    @PUT
    @Timed
    @Path("/{nodeId}/subsystems/{subsystem}/level/{level}")
    @ApiOperation(value = "Set the loglevel of a whole subsystem",
        notes = "Provided level is falling back to DEBUG if it does not exist")
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "No such subsystem.")
    })
    public void setSubsystemLoggerLevel(
        @ApiParam(name = "nodeId", required = true) @PathParam("nodeId") @NotEmpty String nodeId,
        @ApiParam(name = "subsystem", required = true) @PathParam("subsystem") @NotEmpty String subsystemTitle,
        @ApiParam(name = "level", required = true) @PathParam("level") @NotEmpty String level) throws NodeNotFoundException, IOException {
        final Node node = this.nodeService.byNodeId(nodeId);
        final RemoteLoggersResource remoteLoggersResource = this.remoteInterfaceProvider.get(node, this.authenticationToken, RemoteLoggersResource.class);

        remoteLoggersResource.setSubsystemLoggerLevel(subsystemTitle, level).execute();
    }
}
