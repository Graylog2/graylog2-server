/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.rest.resources.cluster;

import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.resources.system.RemoteSystemShutdownResource;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;

/**
 * @deprecated Shutting down nodes using an API request is discouraged in favor of using a service manager to
 *         control the server process.
 */
@RequiresAuthentication
@Path("/cluster/{nodeId}/shutdown")
@Produces(MediaType.APPLICATION_JSON)
@Deprecated
public class ClusterSystemShutdownResource extends ProxiedResource {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterSystemShutdownResource.class);

    @Inject
    public ClusterSystemShutdownResource(NodeService nodeService,
                                         RemoteInterfaceProvider remoteInterfaceProvider,
                                         @Context HttpHeaders httpHeaders,
                                         @Named("proxiedRequestsExecutorService") ExecutorService executorService) throws NodeNotFoundException {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
    }

    @POST
    @Timed
    @AuditEvent(type = AuditEventTypes.NODE_SHUTDOWN_INITIATE)
    public void shutdown(@PathParam("nodeId") String nodeId) throws IOException, NodeNotFoundException {
        LOG.warn(
                "Deprecated API endpoint /cluster/{nodeId}/shutdown was called. Shutting down nodes via the API is " +
                        "discouraged in favor of using a service manager to control the server process.");

        final Node targetNode = nodeService.byNodeId(nodeId);

        RemoteSystemShutdownResource remoteSystemShutdownResource = remoteInterfaceProvider.get(targetNode,
                getAuthenticationToken(),
                RemoteSystemShutdownResource.class);
        final Response response = remoteSystemShutdownResource.shutdown().execute();
        if (response.code() != ACCEPTED.getStatusCode()) {
            LOG.warn("Unable send shut down signal to node {}: {}", nodeId, response.message());
            throw new WebApplicationException(response.message(), BAD_GATEWAY);
        }
    }
}

