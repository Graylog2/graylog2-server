/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.security.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.DBGrantService;
import org.graylog.security.entities.EntityDescriptor;
import org.graylog.security.shares.EntityShareRequest;
import org.graylog.security.shares.EntityShareResponse;
import org.graylog.security.shares.EntitySharesService;
import org.graylog.security.shares.GranteeSharesService;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.PaginationParameters;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.security.RestPermissions.USERS_EDIT;

@Path("/authz/shares")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Authorization/Shares", description = "Manage share permissions on entities")
@RequiresAuthentication
public class EntitySharesResource extends RestResourceWithOwnerCheck {
    private static final Logger LOG = LoggerFactory.getLogger(EntitySharesResource.class);

    private final GRNRegistry grnRegistry;
    private final DBGrantService grantService;
    private final UserService userService;
    private final GranteeSharesService granteeSharesService;
    private final EntitySharesService entitySharesService;

    @Inject
    public EntitySharesResource(GRNRegistry grnRegistry,
                                DBGrantService grantService,
                                UserService userService,
                                GranteeSharesService granteeSharesService,
                                EntitySharesService entitySharesService) {
        this.grnRegistry = grnRegistry;
        this.grantService = grantService;
        this.userService = userService;
        this.granteeSharesService = granteeSharesService;
        this.entitySharesService = entitySharesService;
    }

    @GET
    @ApiOperation(value = "Return shares for a user")
    @Path("user/{userId}")
    public PaginatedResponse<EntityDescriptor> get(@ApiParam(name = "pagination parameters") @BeanParam PaginationParameters paginationParameters,
                                                   @ApiParam(name = "userId", required = true) @PathParam("userId") @NotBlank String userId,
                                                   @ApiParam(name = "capability") @QueryParam("capability") @DefaultValue("") String capabilityFilter,
                                                   @ApiParam(name = "entity_type") @QueryParam("entity_type") @DefaultValue("") String entityTypeFilter) {

        final User user = userService.loadById(userId);
        if (user == null) {
            throw new NotFoundException("Couldn't find user <" + userId + ">");
        }

        if (!isPermitted(USERS_EDIT, user.getName())) {
            throw new ForbiddenException("Couldn't access user <" + userId + ">");
        }

        final GranteeSharesService.SharesResponse response = granteeSharesService.getPaginatedSharesFor(grnRegistry.ofUser(user), paginationParameters, capabilityFilter, entityTypeFilter);

        return PaginatedResponse.create("entities", response.paginatedEntities(), Collections.singletonMap("grantee_capabilities", response.capabilities()));
    }

    @POST
    @ApiOperation(value = "Prepare shares for an entity or collection")
    @Path("entities/{entityGRN}/prepare")
    @NoAuditEvent("This does not change any data")
    public EntityShareResponse prepareShare(@ApiParam(name = "entityGRN", required = true) @PathParam("entityGRN") @NotBlank String entityGRN,
                                            @ApiParam(name = "JSON Body", required = true) @NotNull @Valid EntityShareRequest request) {
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
    @NoAuditEvent("Audit events are created within EntitySharesService")
    public Response updateEntityShares(@ApiParam(name = "entityGRN", required = true) @PathParam("entityGRN") @NotBlank String entityGRN,
                                                  @ApiParam(name = "JSON Body", required = true) @NotNull @Valid EntityShareRequest request) {
        final GRN entity = grnRegistry.parse(entityGRN);
        checkOwnership(entity);

        final EntityShareResponse entityShareResponse = entitySharesService.updateEntityShares(entity, request, requireNonNull(getCurrentUser()));
        if (entityShareResponse.validationResult().failed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(entityShareResponse).build();
        } else {
            return Response.ok(entityShareResponse).build();
        }
    }
}
