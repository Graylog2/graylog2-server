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
package org.graylog2.rest.resources.system.processing;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.processing.ProcessingStatusSummary;
import org.graylog2.shared.rest.resources.ProxiedResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@RequiresAuthentication
@Api(value = "Cluster/Processing/Status")
@Path("/cluster/processing/status")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterProcessingStatusResource extends ProxiedResource {
    @Inject
    public ClusterProcessingStatusResource(NodeService nodeService,
                                           RemoteInterfaceProvider remoteInterfaceProvider,
                                           @Context HttpHeaders httpHeaders,
                                           @Named("proxiedRequestsExecutorService") ExecutorService executorService) {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get processing status from all nodes in the cluster")
    public Map<String, Optional<ProcessingStatusSummary>> getStatus() {
        return getForAllNodes(RemoteSystemProcessingStatusResource::getStatus, createRemoteInterfaceProvider(RemoteSystemProcessingStatusResource.class));
    }

    @GET
    @Path("/persisted")
    @Timed
    @ApiOperation(value = "Get persisted processing status from all nodes in the cluster")
    public Map<String, Optional<ProcessingStatusSummary>> getPersistedStatus() {
        return getForAllNodes(RemoteSystemProcessingStatusResource::getPersistedStatus, createRemoteInterfaceProvider(RemoteSystemProcessingStatusResource.class));
    }
}
