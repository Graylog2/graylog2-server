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
package org.graylog.security.rest;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog.security.shares.EntitySharePrepareRequest;
import org.graylog.security.shares.EntitySharePrepareResponse;
import org.graylog.security.shares.EntityShareRequest;
import org.graylog.security.shares.EntitySharesService;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.utilities.GRN;
import org.graylog2.utilities.GRNRegistry;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Path("shares")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Permissions/Sharing", description = "Manage share permissions on entities")
@RequiresAuthentication
public class EntitySharesResource extends RestResourceWithOwnerCheck {
    private final GRNRegistry grnRegistry;
    private final DBGrantService grantService;
    private final EntitySharesService entitySharesService;

    @Inject
    public EntitySharesResource(GRNRegistry grnRegistry,
                                DBGrantService grantService,
                                EntitySharesService entitySharesService) {
        this.grnRegistry = grnRegistry;
        this.grantService = grantService;
        this.entitySharesService = entitySharesService;
    }

    @GET
    @Path("my")
    public Response get() {
        return Response.ok().build();
    }

    @POST
    @ApiOperation(value = "Prepare shares for an entity or collection")
    @Path("entities/{entityGRN}/prepare")
    @NoAuditEvent("This does not change any data")
    public EntitySharePrepareResponse prepareShare(@ApiParam(name = "entityGRN", required = true) @PathParam("entityGRN") @NotBlank String entityGRN,
                                                   @ApiParam(name = "JSON Body", required = true) @NotNull @Valid EntitySharePrepareRequest request) {
        final GRN grn = grnRegistry.parse(entityGRN);
        checkOwnership(grn);

        // First request would be without "grantees", once the user selects a user/team to share with,
        // we can do a second request including the "grantees". Then we can do the dependency check to
        // fill out "missing_dependencies".
        // This should probably be a POST request with a JSON payload.
        return entitySharesService.prepareShare(grn, request, getCurrentUser(), getSubject());
    }

    @POST
    @ApiOperation(value = "Create / update shares for an entity or collection")
    @Path("entities/{entityGRN}")
    // TODO add description to GraylogServerEventFormatter
    @AuditEvent(type = AuditEventTypes.GRANTS_UPDATE)
    public EntitySharePrepareResponse updateEntityShares(@ApiParam(name = "entityGRN", required = true) @PathParam("entityGRN") @NotBlank String entityGRN,
                                                         @ApiParam(name = "JSON Body", required = true) @NotNull @Valid EntityShareRequest request) {
        final GRN entity = grnRegistry.parse(entityGRN);
        checkOwnership(entity);

        return entitySharesService.updateEntityShares(entity, request, requireNonNull(getCurrentUser()));
    }

    @GET
    @Path("entities/{entityGRN}")
    public Response entityShares(@PathParam("entityGRN") @NotBlank String entityGRN) {
        final GRN grn = grnRegistry.parse(entityGRN);

        checkOwnership(grn);

        // TODO: We need to make the return value of this resource more useful to the frontend
        //       (e.g. returning a list of entities with title, etc.)
        final List<GrantDTO> grants = grantService.getForTarget(grn);

        return Response.ok(ImmutableMap.of("grants", grants)).build();
    }
}
