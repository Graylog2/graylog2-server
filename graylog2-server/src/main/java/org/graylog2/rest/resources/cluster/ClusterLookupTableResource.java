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
import java.util.concurrent.ExecutorService;

/**
 * The primary objective of this API is to provide facilities for managing Lookup Tables on the cluster level.
 * Originally was introduced to perform cluster-wide Cache purging.
 */
@RequiresAuthentication
@Api(value = "Cluster/LookupTable")
@Path("/cluster/system/lookup")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterLookupTableResource extends ProxiedResource {

    @Inject
    public ClusterLookupTableResource(NodeService nodeService,
                                      RemoteInterfaceProvider remoteInterfaceProvider,
                                      @Context HttpHeaders httpHeaders,
                                      @Named("proxiedRequestsExecutorService") ExecutorService executorService) throws NodeNotFoundException {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
    }

    /**
     * Calls {@link org.graylog2.rest.resources.system.lookup.LookupTableResource#performPurge(String, String)}
     * on all active nodes.
     */
    @POST
    @Timed
    @Path("tables/{idOrName}/purge")
    @ApiOperation(value = "Purge Lookup Table Cache on the cluster-wide level")
    @NoAuditEvent("Cache purge only")
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_READ)
    public void performPurge(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName,
                             @ApiParam(name = "key") @QueryParam("key") String key) {
        getForAllNodes(r -> r.performPurge(idOrName, key), createRemoteInterfaceProvider(RemoteLookupTableResource.class));
    }
}
