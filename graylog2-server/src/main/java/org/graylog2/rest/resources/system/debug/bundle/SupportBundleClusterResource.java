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
package org.graylog2.rest.resources.system.debug.bundle;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.shared.rest.resources.ProxiedResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.graylog2.shared.security.RestPermissions.SUPPORTBUNDLE_CREATE;
import static org.graylog2.shared.security.RestPermissions.SUPPORTBUNDLE_READ;

@RequiresAuthentication
@Api(value = "Cluster/Debug/SupportBundle", description = "For collecting cluster wide debugging information, e.g. server logs")
@Path("/cluster/debug/support")
@Produces(MediaType.APPLICATION_JSON)
public class SupportBundleClusterResource extends ProxiedResource {

    @Inject
    public SupportBundleClusterResource(NodeService nodeService,
                                        RemoteInterfaceProvider remoteInterfaceProvider,
                                        @Context HttpHeaders httpHeaders,
                                        @Named("proxiedRequestsExecutorService") ExecutorService executorService) {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
    }

    @GET
    @Path("/manifest")
    @ApiOperation(value = "Get the Support Bundle Manifest from all nodes in the cluster")
    @RequiresPermissions(SUPPORTBUNDLE_READ)
    public Map<String, CallResult<SupportBundleNodeManifest>> getClusterManifest() {
        return requestOnAllNodes(createRemoteInterfaceProvider(RemoteSupportBundleInterface.class), RemoteSupportBundleInterface::getNodeManifest);
    }

    @POST
    @Path("/bundle/build")
    @RequiresPermissions(SUPPORTBUNDLE_CREATE)
    @NoAuditEvent("FIXME") // TODO
    public Response buildBundle() throws IOException {
        return requestOnLeader(RemoteSupportBundleInterface::buildSupportBundle, createRemoteInterfaceProvider(RemoteSupportBundleInterface.class)).entity().get();
    }
}
