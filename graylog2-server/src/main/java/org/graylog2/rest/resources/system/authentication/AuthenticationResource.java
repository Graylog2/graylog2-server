package org.graylog2.rest.resources.system.authentication;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.AuthenticationConfig;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@RequiresAuthentication
@Path("/system/authentication")
@Api(value = "System/Authentication", description = "Manage authentication providers")
@Produces(MediaType.APPLICATION_JSON)
public class AuthenticationResource extends RestResource {

    private final ClusterConfigService clusterConfigService;
    private final Map<String, AuthenticatingRealm> availableRealms;

    @Inject
    public AuthenticationResource(final ClusterConfigService clusterConfigService,
                                  final Map<String, AuthenticatingRealm> availableRealms) {

        this.clusterConfigService = clusterConfigService;
        this.availableRealms = availableRealms;
    }

    @GET
    @Path("config")
    @ApiOperation("Retrieve authentication providers configuration")
    public AuthenticationConfig getAuthenticators() {
        checkPermission(RestPermissions.CLUSTER_CONFIG_ENTRY_READ);
        final AuthenticationConfig config = clusterConfigService.getOrDefault(AuthenticationConfig.class,
                                                                              AuthenticationConfig.defaultInstance());
        return config.withRealms(availableRealms.keySet());
    }

    @PUT
    @Path("config")
    @ApiOperation("Update authentication providers configuration")
    public AuthenticationConfig create(@ApiParam(name = "config", required = true) final AuthenticationConfig config) {
        checkPermission(RestPermissions.CLUSTER_CONFIG_ENTRY_READ);

        clusterConfigService.write(config);
        return clusterConfigService.getOrDefault(AuthenticationConfig.class, config);
    }
}
