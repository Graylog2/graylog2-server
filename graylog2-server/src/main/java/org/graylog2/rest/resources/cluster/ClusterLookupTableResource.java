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
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.resources.system.RemoteLookupTableResource;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

/**
 * The primary objective of this API is to provide facilities for managing Lookup Tables on the cluster level.
 * Originally was introduced to perform cluster-wide Cache purging.
 */
@RequiresAuthentication
@Api(value = "Cluster/LookupTable", tags = {CLOUD_VISIBLE})
@Path("/cluster/system/lookup")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterLookupTableResource extends ProxiedResource {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterLookupTableResource.class);

    @Inject
    public ClusterLookupTableResource(NodeService nodeService,
                                      RemoteInterfaceProvider remoteInterfaceProvider,
                                      @Context HttpHeaders httpHeaders,
                                      @Named("proxiedRequestsExecutorService") ExecutorService executorService) throws NodeNotFoundException {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
    }

    /**
     * Calls {@link org.graylog2.rest.resources.system.lookup.LookupTableResource#performPurge(String, String)}
     * on all active nodes and returns results per node
     */
    @POST
    @Timed
    @Path("tables/{idOrName}/purge")
    @ApiOperation(value = "Purge Lookup Table Cache on the cluster-wide level")
    @NoAuditEvent("Cache purge only")
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_READ)
    public Map<String, CallResult<Void>> performPurge(
            @ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName,
            @ApiParam(name = "key") @QueryParam("key") String key) {
        return requestOnAllNodes(RemoteLookupTableResource.class, client -> client.performPurge(idOrName, key));
    }
}
