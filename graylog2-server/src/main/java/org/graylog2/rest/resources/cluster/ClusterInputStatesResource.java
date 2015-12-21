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
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.graylog2.rest.models.system.inputs.responses.InputStateSummary;
import org.graylog2.rest.models.system.inputs.responses.InputStatesList;
import org.graylog2.rest.resources.system.inputs.RemoteInputStatesResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit.Response;

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
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "Cluster/InputState", description = "Cluster-wide input states")
@Path("/cluster/inputstates")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterInputStatesResource {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterInputStatesResource.class);

    private final NodeService nodeService;
    private final RemoteInterfaceProvider remoteInterfaceProvider;
    private final String authenticationToken;

    @Inject
    public ClusterInputStatesResource(NodeService nodeService,
                                      RemoteInterfaceProvider remoteInterfaceProvider,
                                      @Context HttpHeaders httpHeaders) throws NodeNotFoundException {
        this.nodeService = nodeService;
        this.remoteInterfaceProvider = remoteInterfaceProvider;

        final List<String> authenticationTokens = httpHeaders.getRequestHeader("Authorization");
        if (authenticationTokens != null && authenticationTokens.size() >= 1) {
            this.authenticationToken = authenticationTokens.get(0);
        } else {
            this.authenticationToken = null;
        }
    }

    @GET
    @Timed
    @ApiOperation(value = "Get all input states")
    @RequiresPermissions(RestPermissions.INPUTS_READ)
    public Map<String, Set<InputStateSummary>> get() {
        final Map<String, Node> nodes = nodeService.allActive();
        final Map<String, Set<InputStateSummary>> result = nodes.entrySet()
                .stream()
                .parallel()
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> {
                    final RemoteInputStatesResource remoteInputStatesResource = remoteInterfaceProvider.get(entry.getValue(),
                            this.authenticationToken,
                            RemoteInputStatesResource.class);
                    try {
                        final Response<InputStatesList> response = remoteInputStatesResource.list().execute();
                        if (response.isSuccess()) {
                            return response.body().states();
                        } else {
                            LOG.warn("Unable to fetch input states from node " + entry.getKey() + ": " + response.message());
                        }
                    } catch (IOException e) {
                        LOG.warn("Unable to fetch input states from node " + entry.getKey() + ": ", e);
                    }
                    return Collections.emptySet();
                }));
        return result;
    }

    @PUT
    @Path("/{inputId}")
    @Timed
    @ApiOperation(value = "Start or restart specified input in all nodes")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input."),
    })
    public Map<String, InputCreated> start(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) {
        final Map<String, Node> nodes = nodeService.allActive();
        final Map<String, InputCreated> result = nodes.entrySet()
                .stream()
                .parallel()
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> {
                    final RemoteInputStatesResource remoteInputStatesResource = remoteInterfaceProvider.get(entry.getValue(),
                            this.authenticationToken,
                            RemoteInputStatesResource.class);
                    try {
                        final Response<InputCreated> response = remoteInputStatesResource.start(inputId).execute();
                        if (response.isSuccess()) {
                            return response.body();
                        } else {
                            LOG.warn("Unable to start input on node " + entry.getKey() + ": " + response.message());
                        }
                    } catch (IOException e) {
                        LOG.warn("Unable to start input on node " + entry.getKey() + ": ", e);
                    }
                    return null;
                }));
        return result;
    }

    @DELETE
    @Path("/{inputId}")
    @Timed
    @ApiOperation(value = "Stop specified input in all nodes")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input."),
    })
    public Map<String, InputDeleted> stop(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) {
        final Map<String, Node> nodes = nodeService.allActive();
        final Map<String, InputDeleted> result = nodes.entrySet()
                .stream()
                .parallel()
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> {
                    final RemoteInputStatesResource remoteInputStatesResource = remoteInterfaceProvider.get(entry.getValue(),
                            this.authenticationToken,
                            RemoteInputStatesResource.class);
                    try {
                        final Response<InputDeleted> response = remoteInputStatesResource.stop(inputId).execute();
                        if (response.isSuccess()) {
                            return response.body();
                        } else {
                            LOG.warn("Unable to stop input on node " + entry.getKey() + ": " + response.message());
                        }
                    } catch (IOException e) {
                        LOG.warn("Unable to stop input on node " + entry.getKey() + ": ", e);
                    }
                    return null;
                }));
        return result;
    }
}
