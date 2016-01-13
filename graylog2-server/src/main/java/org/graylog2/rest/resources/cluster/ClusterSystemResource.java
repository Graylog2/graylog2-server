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
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.rest.resources.system.RemoteSystemResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "Cluster", description = "System information of all nodes in the cluster")
@Path("/cluster")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterSystemResource {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterSystemResource.class);

    private final NodeService nodeService;
    private final RemoteInterfaceProvider remoteInterfaceProvider;
    private final String authenticationToken;

    @Inject
    public ClusterSystemResource(NodeService nodeService,
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
    @ApiOperation(value = "Get system overview of all Graylog nodes")
    public Map<String, Optional<SystemOverviewResponse>> get() {
        final Map<String, Node> nodes = nodeService.allActive();
        return nodes.entrySet()
                .stream()
                .parallel()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    final RemoteSystemResource remoteSystemResource = remoteInterfaceProvider.get(entry.getValue(),
                            this.authenticationToken,
                            RemoteSystemResource.class);
                    try {
                        final Response<SystemOverviewResponse> response = remoteSystemResource.system().execute();
                        if (response.isSuccess()) {
                            return Optional.of(response.body());
                        } else {
                            LOG.warn("Unable to fetch system overview from node " + entry.getKey() + ": " + response.message());
                        }
                    } catch (IOException e) {
                        LOG.warn("Unable to fetch system overview from node " + entry.getKey() + ": ", e);
                    }
                    return Optional.absent();
                }));
    }
}

