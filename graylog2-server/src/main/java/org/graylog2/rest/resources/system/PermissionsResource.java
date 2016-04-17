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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.graylog2.rest.models.system.responses.ReaderPermissionResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.Permissions;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.Collection;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@RequiresAuthentication
@Api(value = "System/Permissions", description = "Retrieval of system permissions.")
@Path("/system/permissions")
@Produces(APPLICATION_JSON)
public class PermissionsResource extends RestResource {
    private final Permissions permissions;

    @Inject
    public PermissionsResource(final Permissions permissions) {
        this.permissions = permissions;
    }

    @GET
    @Timed
    @RequiresGuest // turns off authentication for this action
    @ApiOperation(value = "Get all available user permissions.")
    public Map<String, Map<String, Collection<String>>> permissions() {
        return ImmutableMap.of("permissions", permissions.allPermissionsMap());
    }

    @GET
    @Timed
    @RequiresGuest
    @ApiOperation(value = "Get the initial permissions assigned to a reader account")
    @Path("reader/{username}")
    @Produces(APPLICATION_JSON)
    public ReaderPermissionResponse readerPermissions(
            @ApiParam(name = "username", required = true)
            @PathParam("username") String username) {
        return ReaderPermissionResponse.create(
                Ordering.natural().sortedCopy(permissions.userSelfEditPermissions(username)));
    }
}
