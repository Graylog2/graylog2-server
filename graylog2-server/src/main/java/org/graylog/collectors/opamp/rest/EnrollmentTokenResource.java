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
package org.graylog.collectors.opamp.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.bson.conversions.Bson;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.db.EnrollmentTokenCreator;
import org.graylog.collectors.db.EnrollmentTokenDTO;
import org.graylog.collectors.db.FleetDTO;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;
import org.graylog.plugins.sidecar.permissions.SidecarRestPermissions;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.filtering.ComputedFieldRegistry;
import org.graylog2.database.filtering.DbQueryCreator;
import org.graylog2.database.filtering.DbSortResolver;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.List;

@Tag(name = "OpAMP Enrollment", description = "OpAMP agent enrollment management")
@Path("/opamp/enrollment-tokens")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class EnrollmentTokenResource extends RestResource {

    private static final String DEFAULT_SORT_FIELD = EnrollmentTokenDTO.FIELD_CREATED_AT;
    private static final String DEFAULT_SORT_DIRECTION = "desc";

    private static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id(EnrollmentTokenDTO.FIELD_FLEET_ID).title("Fleet")
                    .relatedCollection(FleetService.COLLECTION_NAME)
                    .relatedIdentifier("_id")
                    .relatedDisplayFields(List.of(FleetDTO.FIELD_NAME))
                    .relatedDisplayTemplate("{name}")
                    .sortable(false)
                    .searchable(true)
                    .filterable(true)
                    .build(),
            EntityAttribute.builder().id(EnrollmentTokenDTO.FIELD_CREATED_BY).title("Created By").sortable(false).build(),
            EntityAttribute.builder().id(EnrollmentTokenDTO.FIELD_CREATED_AT).title("Created At").sortable(true).build(),
            EntityAttribute.builder().id(EnrollmentTokenDTO.FIELD_EXPIRES_AT).title("Expires").sortable(true).build(),
            EntityAttribute.builder().id(EnrollmentTokenDTO.FIELD_USAGE_COUNT).title("Usages").sortable(true).build(),
            EntityAttribute.builder().id(EnrollmentTokenDTO.FIELD_LAST_USED_AT).title("Last Used").sortable(true).build()
    );

    private static final EntityDefaults DEFAULTS = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.DESC))
            .build();

    private final EnrollmentTokenService enrollmentTokenService;
    private final CollectorsConfigService collectorsConfigService;
    private final FleetService fleetService;
    private final DbQueryCreator dbQueryCreator;

    @Inject
    public EnrollmentTokenResource(EnrollmentTokenService enrollmentTokenService,
                                   CollectorsConfigService collectorsConfigService,
                                   FleetService fleetService,
                                   ComputedFieldRegistry computedFieldRegistry) {
        this.enrollmentTokenService = enrollmentTokenService;
        this.collectorsConfigService = collectorsConfigService;
        this.fleetService = fleetService;
        this.dbQueryCreator = new DbQueryCreator(EnrollmentTokenDTO.FIELD_FLEET_ID, ATTRIBUTES, computedFieldRegistry);
    }

    // TODO: Add @AuditEvent for security audit logging of token creation
    @NoAuditEvent("TODO")
    @POST
    @Operation(summary = "Create an enrollment token for OpAMP agents")
    // TODO: Replace with proper OpAMP permissions and verify target fleet exists and caller has access
    @RequiresPermissions(SidecarRestPermissions.SIDECARS_CREATE)
    public EnrollmentTokenResponse createToken(
            @RequestBody(description = "Enrollment token creation request")
            @Valid @NotNull CreateEnrollmentTokenRequest request) {
        collectorsConfigService.get().orElseThrow(() ->
                new BadRequestException("Collectors must be configured before creating enrollment tokens. " +
                        "Configure collectors at /api/collectors/config first.")
        );

        if (fleetService.get(request.fleetId()).isEmpty()) {
            throw new BadRequestException("Fleet not found: " + request.fleetId());
        }
        final var user = getCurrentUser();
        final var creator = new EnrollmentTokenCreator(user.getId(), user.getName());
        return enrollmentTokenService.createToken(request, creator);
    }

    // TODO: Add fleet-aware authorization — filter results to fleets the caller may read
    @GET
    @Operation(summary = "List enrollment tokens")
    @RequiresPermissions(SidecarRestPermissions.SIDECARS_CREATE)
    public PageListResponse<EnrollmentTokenDTO> list(
            @Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
            @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
            @Parameter(name = "filters") @QueryParam("filters") List<String> filters,
            @Parameter(name = "sort", description = "The field to sort the result on",
                       schema = @Schema(allowableValues = {"created_at", "expires_at", "usage_count", "last_used_at"}))
            @QueryParam("sort") @DefaultValue(DEFAULT_SORT_FIELD) String sort,
            @Parameter(name = "order", description = "The sort direction",
                       schema = @Schema(allowableValues = {"asc", "desc"}))
            @QueryParam("order") @DefaultValue(DEFAULT_SORT_DIRECTION) SortOrder order) {
        final Bson dbQuery = dbQueryCreator.createDbQuery(filters, query);
        final var resolvedSort = DbSortResolver.resolve(ATTRIBUTES, sort, order);
        final var list = enrollmentTokenService.findPaginated(dbQuery, resolvedSort, page, perPage);

        return PageListResponse.create(query, list.pagination(), list.pagination().total(),
                sort, order, list.stream().toList(), ATTRIBUTES, DEFAULTS);
    }

    // TODO: Add @AuditEvent for security audit logging of token deletion
    // TODO: Authorize against the token's fleet, not only a global permission
    @NoAuditEvent("TODO")
    @DELETE
    @Path("/{tokenId}")
    @Operation(summary = "Delete an enrollment token")
    @RequiresPermissions(SidecarRestPermissions.SIDECARS_CREATE)
    public Response delete(@Parameter(name = "tokenId", required = true) @PathParam("tokenId") String tokenId) {
        if (!org.bson.types.ObjectId.isValid(tokenId)) {
            throw new BadRequestException("Invalid token ID format");
        }
        if (!enrollmentTokenService.delete(tokenId)) {
            throw new jakarta.ws.rs.NotFoundException("Enrollment token not found");
        }
        return Response.noContent().build();
    }
}
