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
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.security.authservice.AuthServiceBackendDTO;
import org.graylog.security.authservice.AuthServiceBackendUsageCheck;
import org.graylog.security.authservice.DBAuthServiceBackendService;
import org.graylog.security.authservice.GlobalAuthServiceConfig;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.rest.ValidationFailureException;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.rest.PaginationParameters;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

@Path("/system/authentication/services/backends")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "System/Authentication/Services/Backends", description = "Manage authentication service backends")
@RequiresAuthentication
public class AuthServiceBackendsResource extends RestResource {
    private final DBAuthServiceBackendService dbService;
    private final GlobalAuthServiceConfig globalAuthServiceConfig;
    private final AuthServiceBackendUsageCheck usageCheck;

    @Inject
    public AuthServiceBackendsResource(DBAuthServiceBackendService dbService,
                                       GlobalAuthServiceConfig globalAuthServiceConfig,
                                       AuthServiceBackendUsageCheck usageCheck) {
        this.dbService = dbService;
        this.globalAuthServiceConfig = globalAuthServiceConfig;
        this.usageCheck = usageCheck;
    }

    @GET
    @ApiOperation("Returns available authentication service backends")
    public PaginatedResponse<AuthServiceBackendDTO> list(@ApiParam(name = "pagination parameters") @BeanParam PaginationParameters paginationParameters) {
        final AuthServiceBackendDTO activeBackendConfig = globalAuthServiceConfig.getActiveBackendConfig()
                .filter(this::checkReadPermission)
                .orElse(null);
        final PaginatedList<AuthServiceBackendDTO> list = dbService.findPaginated(paginationParameters, this::checkReadPermission);

        return PaginatedResponse.create(
                "backends",
                list,
                Collections.singletonMap("active_backend", activeBackendConfig)
        );
    }

    @GET
    @Path("{backendId}")
    @ApiOperation("Returns the authentication service backend for the given ID")
    public Response get(@ApiParam(name = "backendId", required = true) @PathParam("backendId") @NotBlank String backendId) {
        checkPermission(RestPermissions.AUTH_SERVICE_BACKEND_READ, backendId);

        return toResponse(loadConfig(backendId));
    }

    @POST
    @ApiOperation("Creates a new authentication service backend")
    @RequiresPermissions(RestPermissions.AUTH_SERVICE_BACKEND_CREATE)
    public Response create(@ApiParam(name = "JSON body", required = true) @NotNull AuthServiceBackendDTO newConfig) {
        validateConfig(newConfig);

        return toResponse(dbService.save(newConfig));
    }

    @PUT
    @Path("{backendId}")
    @ApiOperation("Creates a new authentication service backend")
    public Response update(@ApiParam(name = "backendId", required = true) @PathParam("backendId") @NotBlank String backendId,
                           @ApiParam(name = "JSON body", required = true) @NotNull AuthServiceBackendDTO updatedConfig) {
        checkPermission(RestPermissions.AUTH_SERVICE_BACKEND_EDIT, backendId);
        validateConfig(updatedConfig);

        final AuthServiceBackendDTO currentConfig = loadConfig(backendId);

        return toResponse(updatedConfig.withId(currentConfig.id()));
    }

    @DELETE
    @Path("{backendId}")
    @ApiOperation("Delete authentication service backend")
    public void delete(@ApiParam(name = "backendId", required = true) @PathParam("backendId") @NotBlank String backendId) {
        checkPermission(RestPermissions.AUTH_SERVICE_BACKEND_DELETE, backendId);

        final AuthServiceBackendDTO config = loadConfig(backendId);

        if (usageCheck.isAuthServiceInUse(backendId)) {
            throw new BadRequestException("Authentication service backend <" + backendId + "> is still in use");
        }
        dbService.delete(config.id());
    }

    private boolean checkReadPermission(AuthServiceBackendDTO config) {
        return isPermitted(RestPermissions.AUTH_SERVICE_BACKEND_READ, config.id());
    }

    private AuthServiceBackendDTO loadConfig(String backendId) {
        checkArgument(!isNullOrEmpty(backendId), "backendId cannot be null or empty");

        return dbService.get(backendId)
                .orElseThrow(() -> new NotFoundException("Couldn't find auth service backend " + backendId));
    }

    private void validateConfig(AuthServiceBackendDTO config) {
        final ValidationResult result = config.validate();

        if (result.failed()) {
            throw new ValidationFailureException(result);
        }
    }

    private Response toResponse(Object entity) {
        return Response.ok(Collections.singletonMap("backend", entity)).build();
    }
}

