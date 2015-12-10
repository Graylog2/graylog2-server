package org.graylog2.rest.resources.cluster;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.inputs.responses.InputStateSummary;
import org.graylog2.rest.models.system.inputs.responses.InputStatesList;
import org.graylog2.rest.resources.system.inputs.RemoteInputStatesResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit.Response;

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
        final Map<String, Set<InputStateSummary>> result = nodes.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> {
            final RemoteInputStatesResource remoteInputStatesResource = remoteInterfaceProvider.get(entry.getValue(),
                    this.authenticationToken,
                    RemoteInputStatesResource.class);
            try {
                final Response<InputStatesList> response = remoteInputStatesResource.list().execute();
                if (response.isSuccess()) {
                    return response.body().states();
                } else {
                    LOG.warn("Unable to fetch system jobs from node " + entry.getKey() + ": " + response);
                }
            } catch (IOException e) {
                LOG.warn("Unable to fetch system jobs from node " + entry.getKey() + ": ", e);
            }
            return null;
        }));
        return result;
    }
}
