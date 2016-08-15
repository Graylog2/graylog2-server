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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.auditlog.AuditActions;
import org.graylog2.auditlog.jersey.AuditEvent;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.rest.resources.system.RemoteDeflectorResource;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

@RequiresAuthentication
@Api(value = "Cluster/Deflector", description = "Cluster-wide deflector handling")
@Path("/cluster/deflector")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterDeflectorResource extends ProxiedResource {
    @Inject
    public ClusterDeflectorResource(@Context HttpHeaders httpHeaders,
                                    NodeService nodeService,
                                    RemoteInterfaceProvider remoteInterfaceProvider) {
        super(httpHeaders, nodeService, remoteInterfaceProvider);
    }

    @POST
    @Timed
    @ApiOperation(value = "Finds master node and triggers deflector cycle")
    @Path("/cycle")
    @AuditEvent(action = AuditActions.ES_WRITE_INDEX_UPDATE)
    public void cycle() throws IOException {
        final Optional<Node> master = nodeService.allActive().values().stream().filter(Node::isMaster).findFirst();
        if (!master.isPresent()) {
            throw new ServiceUnavailableException("No master present.");
        }
        final Function<String, Optional<RemoteDeflectorResource>> remoteInterfaceProvider = createRemoteInterfaceProvider(RemoteDeflectorResource.class);
        final Optional<RemoteDeflectorResource> deflectorResource = remoteInterfaceProvider.apply(master.get().getNodeId());

        deflectorResource
            .orElseThrow(() -> new InternalServerErrorException("Unable to get remote deflector resource."))
            .cycle().execute();
    }
}
