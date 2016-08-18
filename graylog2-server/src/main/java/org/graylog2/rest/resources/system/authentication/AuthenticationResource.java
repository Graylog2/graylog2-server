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
package org.graylog2.rest.resources.system.authentication;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.AuthenticationConfig;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

import static org.graylog2.shared.security.RestPermissions.AUTHENTICATION_EDIT;
import static org.graylog2.shared.security.RestPermissions.CLUSTER_CONFIG_ENTRY_READ;

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
    @RequiresPermissions(CLUSTER_CONFIG_ENTRY_READ)
    public AuthenticationConfig getAuthenticators() {
        final AuthenticationConfig config = clusterConfigService.getOrDefault(AuthenticationConfig.class,
                                                                              AuthenticationConfig.defaultInstance());
        return config.withRealms(availableRealms.keySet());
    }

    @PUT
    @Path("config")
    @ApiOperation("Update authentication providers configuration")
    @RequiresPermissions({CLUSTER_CONFIG_ENTRY_READ, AUTHENTICATION_EDIT})
    @AuditEvent(type = AuditEventTypes.AUTHENTICATION_PROVIDER_CONFIGURATION_UPDATE)
    public AuthenticationConfig create(@ApiParam(name = "config", required = true) final AuthenticationConfig config) {
        clusterConfigService.write(config);
        return clusterConfigService.getOrDefault(AuthenticationConfig.class, config);
    }
}
