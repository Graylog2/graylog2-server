package org.graylog2.rest.resources.system.authentication;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.AuthenticationConfig;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@RequiresAuthentication
@Path("/system/authentication")
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
    public AuthenticationConfig getAuthenticators() {
        checkPermission(RestPermissions.CLUSTER_CONFIG_ENTRY_READ);
        final AuthenticationConfig config = clusterConfigService.getOrDefault(AuthenticationConfig.class,
                                                                              AuthenticationConfig.defaultInstance());
        return config;
    }
}
