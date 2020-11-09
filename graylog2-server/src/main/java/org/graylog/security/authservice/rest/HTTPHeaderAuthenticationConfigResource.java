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
package org.graylog.security.authservice.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.headerauth.HTTPHeaderAuthConfig;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/system/authentication/http-header-auth-config")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "System/Authentication/HTTPHeaderAuthConfig", description = "Manage the HTTP header authentication configuration")
@RequiresAuthentication
public class HTTPHeaderAuthenticationConfigResource {
    private final ClusterConfigService clusterConfigService;

    @Inject
    public HTTPHeaderAuthenticationConfigResource(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @GET
    @ApiOperation("Get HTTP header authentication config")
    @RequiresPermissions(RestPermissions.AUTH_HTTP_HEADER_CONFIG_READ)
    public HTTPHeaderAuthConfig getConfig() {
        return loadConfig();
    }

    @PUT
    @ApiOperation("Update HTTP header authentication config")
    @RequiresPermissions(RestPermissions.AUTH_HTTP_HEADER_CONFIG_EDIT)
    @AuditEvent(type = AuditEventTypes.AUTHENTICATION_HTTP_HEADER_CONFIG_UPDATE)
    public HTTPHeaderAuthConfig updateConfig(@Valid HTTPHeaderAuthConfig config) {
        clusterConfigService.write(config);
        return loadConfig();
    }

    private HTTPHeaderAuthConfig loadConfig() {
        return clusterConfigService.getOrDefault(HTTPHeaderAuthConfig.class, HTTPHeaderAuthConfig.createDisabled());
    }
}
