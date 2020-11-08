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
import org.graylog.security.authservice.AuthServiceBackendDTO;
import org.graylog.security.authservice.DBAuthServiceBackendService;
import org.graylog.security.authservice.GlobalAuthServiceConfig;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Path("/system/authentication/services")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "System/Authentication/Services", description = "Manage authentication services")
@RequiresAuthentication
public class AuthServicesResource extends RestResource {
    private final GlobalAuthServiceConfig authServiceConfig;
    private final DBAuthServiceBackendService backendService;

    @Inject
    public AuthServicesResource(GlobalAuthServiceConfig authServiceConfig,
                                DBAuthServiceBackendService backendService) {
        this.authServiceConfig = authServiceConfig;
        this.backendService = backendService;
    }

    @GET
    @Path("active-backend")
    @ApiOperation("Get active authentication service backend")
    @RequiresPermissions(RestPermissions.AUTH_SERVICE_GLOBAL_CONFIG_READ)
    public Response get() {
        final Optional<AuthServiceBackendDTO> activeConfig = getActiveBackendConfig();

        // We cannot use an ImmutableMap because the backend value can be null
        final Map<String, Object> response = new HashMap<>();
        response.put("backend", activeConfig.orElse(null));
        response.put("context", Collections.singletonMap("backends_total", backendService.countBackends()));

        return Response.ok(response).build();
    }

    private Optional<AuthServiceBackendDTO> getActiveBackendConfig() {
        final Optional<AuthServiceBackendDTO> activeConfig = authServiceConfig.getActiveBackendConfig();

        activeConfig.ifPresent(backend -> checkPermission(RestPermissions.AUTH_SERVICE_BACKEND_READ, backend.id()));

        return activeConfig;
    }
}
