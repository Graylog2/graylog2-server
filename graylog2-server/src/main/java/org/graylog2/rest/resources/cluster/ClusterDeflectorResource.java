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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.rest.resources.system.RemoteDeflectorResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "Cluster/Deflector", description = "Cluster-wide deflector handling", tags = {CLOUD_VISIBLE})
@Path("/cluster/deflector")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterDeflectorResource extends ProxiedResource {
    @Inject
    public ClusterDeflectorResource(@Context HttpHeaders httpHeaders,
                                    NodeService nodeService,
                                    RemoteInterfaceProvider remoteInterfaceProvider,
                                    @Named("proxiedRequestsExecutorService") ExecutorService executorService) {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
    }

    @POST
    @Timed
    @ApiOperation(value = "Finds leader node and triggers deflector cycle")
    @Path("/cycle")
    @NoAuditEvent("this is a proxy resource, the event will be triggered on the individual nodes")
    public void cycle() throws IOException {
        requestOnLeader(RemoteDeflectorResource::cycle, RemoteDeflectorResource.class);
    }

    @POST
    @Timed
    @ApiOperation(value = "Finds leader node and triggers deflector cycle")
    @Path("/{indexSetId}/cycle")
    @NoAuditEvent("this is a proxy resource, the event will be triggered on the individual nodes")
    public void cycle(@ApiParam(name = "indexSetId") @PathParam("indexSetId") String indexSetId) throws IOException {
        requestOnLeader(c -> c.cycleIndexSet(indexSetId), RemoteDeflectorResource.class);
    }
}
