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

import com.google.common.primitives.Ints;
import com.mongodb.client.model.Filters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.CollectorsPermissions;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.db.EnrollmentTokenCreator;
import org.graylog.collectors.db.EnrollmentTokenDTO;
import org.graylog.collectors.db.FleetDTO;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.filtering.ComputedFieldRegistry;
import org.graylog2.database.filtering.DbQueryCreator;
import org.graylog2.database.filtering.DbSortResolver;
import org.graylog2.rest.bulk.model.BulkOperationRequest;
import org.graylog2.rest.bulk.model.BulkOperationResponse;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQueryField;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Tag(name = "OpAMP Enrollment", description = "OpAMP agent enrollment management")
@Path("/opamp/enrollment-tokens")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class EnrollmentTokenResource extends RestResource {

    private static final String DEFAULT_SORT_FIELD = EnrollmentTokenDTO.FIELD_CREATED_AT;
    private static final String DEFAULT_SORT_DIRECTION = "desc";

    private static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id(EnrollmentTokenDTO.FIELD_NAME).title("Name").sortable(true).searchable(true)
                    .filterable(true).build(),
            // See CollectorInstancesResource for explanation of the type(OBJECT_ID) + bsonFilterCreator workaround.
            EntityAttribute.builder()
                    .id(EnrollmentTokenDTO.FIELD_FLEET_ID)
                    .title("Fleet")
                    .relatedCollection(FleetService.COLLECTION_NAME)
                    .relatedIdentifier("_id")
                    .relatedDisplayFields(List.of(FleetDTO.FIELD_NAME))
                    .relatedDisplayTemplate("{name}")
                    .type(SearchQueryField.Type.OBJECT_ID)
                    .bsonFilterCreator((name, value) -> Filters.eq(name, value.getValue().toString()))
                    .sortable(false)
                    .searchable(false)
                    .filterable(true)
                    .build(),
            EntityAttribute.builder()
                    .id(EnrollmentTokenDTO.FIELD_CREATED_BY)
                    .title("Created By")
                    .dbField(EnrollmentTokenDTO.FIELD_CREATED_BY + "." + EnrollmentTokenCreator.FIELD_USERNAME)
                    .bsonFilterCreator((name, value) -> Filters.eq(
                            EnrollmentTokenDTO.FIELD_CREATED_BY + "." + EnrollmentTokenCreator.FIELD_USERNAME,
                            value.getValue().toString()))
                    .sortable(true)
                    .searchable(false)
                    .filterable(true)
                    .build(),
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
    private final AuditEventSender auditEventSender;

    @Inject
    public EnrollmentTokenResource(EnrollmentTokenService enrollmentTokenService,
                                   CollectorsConfigService collectorsConfigService,
                                   FleetService fleetService,
                                   ComputedFieldRegistry computedFieldRegistry,
                                   AuditEventSender auditEventSender) {
        this.enrollmentTokenService = enrollmentTokenService;
        this.collectorsConfigService = collectorsConfigService;
        this.fleetService = fleetService;
        this.dbQueryCreator = new DbQueryCreator(EnrollmentTokenDTO.FIELD_NAME, ATTRIBUTES, computedFieldRegistry);
        this.auditEventSender = auditEventSender;
    }

    @POST
    @Operation(summary = "Create an enrollment token for OpAMP agents")
    @NoAuditEvent("inline for ID")
    public EnrollmentTokenResponse createToken(
            @RequestBody(description = "Enrollment token creation request")
            @Valid @NotNull CreateEnrollmentTokenRequest request) {
        collectorsConfigService.get().orElseThrow(() ->
                new BadRequestException("Collectors must be configured before creating enrollment tokens. " +
                        "Configure collectors at /api/collectors/config first.")
        );

        checkPermission(CollectorsPermissions.FLEET_INSTANCE_ASSIGN, request.fleetId());

        if (fleetService.get(request.fleetId()).isEmpty()) {
            throw new BadRequestException("Fleet not found: " + request.fleetId());
        }
        final var user = getCurrentUser();
        final var creator = new EnrollmentTokenCreator(user.getId(), user.getName());
        final EnrollmentTokenResponse token = enrollmentTokenService.createToken(request, creator);

        auditEventSender.success(getAuditActor(), AuditEventTypes.OPAMP_ENROLLMENT_TOKEN_CREATE, Map.of(
                "tokenId", Objects.requireNonNull(token.id()),
                "name", request.name(),
                "expiresAt", Objects.requireNonNullElse(token.expiresAt(), "never"),
                "fleetId", request.fleetId()
        ));
        return token;
    }

    @GET
    @Operation(summary = "List enrollment tokens")
    public PageListResponse<EnrollmentTokenDTO> list(
            @Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
            @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
            @Parameter(name = "filters") @QueryParam("filters") List<String> filters,
            @Parameter(name = "sort", description = "The field to sort the result on",
                       schema = @Schema(allowableValues = {EnrollmentTokenDTO.FIELD_NAME,
                               EnrollmentTokenDTO.FIELD_CREATED_BY, EnrollmentTokenDTO.FIELD_CREATED_AT,
                               EnrollmentTokenDTO.FIELD_EXPIRES_AT, EnrollmentTokenDTO.FIELD_USAGE_COUNT,
                               EnrollmentTokenDTO.FIELD_LAST_USED_AT}))
            @QueryParam("sort") @DefaultValue(DEFAULT_SORT_FIELD) String sort,
            @Parameter(name = "order", description = "The sort direction",
                       schema = @Schema(allowableValues = {"asc", "desc"}))
            @QueryParam("order") @DefaultValue(DEFAULT_SORT_DIRECTION) SortOrder order) {
        final Bson dbQuery = dbQueryCreator.createDbQuery(filters, query);
        final var resolvedSort = DbSortResolver.resolve(ATTRIBUTES, sort, order);
        final var list = enrollmentTokenService.findPaginated(dbQuery, resolvedSort,
                page, perPage, dto -> isPermitted(CollectorsPermissions.FLEET_INSTANCE_ASSIGN, dto.fleetId()));

        return PageListResponse.create(query, list.pagination(), list.pagination().total(),
                sort, order, list.stream().toList(), ATTRIBUTES, DEFAULTS);
    }

    @DELETE
    @Path("/{tokenId}")
    @Operation(summary = "Delete an enrollment token")
    @NoAuditEvent("inline")
    public Response delete(@Parameter(name = "tokenId", required = true) @PathParam("tokenId") String tokenId) {
        if (!ObjectId.isValid(tokenId)) {
            throw new BadRequestException("Invalid token ID format");
        }

        final Optional<EnrollmentTokenDTO> token = enrollmentTokenService.findOne(tokenId);
        if (token.isEmpty()) {
            throw new NotFoundException("Enrollment token not found");
        }
        final EnrollmentTokenDTO dto = token.get();
        checkPermission(CollectorsPermissions.FLEET_INSTANCE_ASSIGN, dto.fleetId());

        if (!enrollmentTokenService.delete(tokenId)) {
            throw new NotFoundException("Enrollment token not found");
        }
        auditEventSender.success(getAuditActor(), AuditEventTypes.OPAMP_ENROLLMENT_TOKEN_DELETE, Map.of(
                "tokenId", Objects.requireNonNull(dto.id()),
                "name", dto.name(),
                "expiresAt", Objects.requireNonNullElse(dto.expiresAt(), "never"),
                "fleetId", dto.fleetId()
        ));
        return Response.noContent().build();
    }

    @POST
    @Path("/bulk_delete")
    @Operation(summary = "Bulk delete enrollment tokens")
    @NoAuditEvent("inline for bulk")
    public BulkOperationResponse bulkDelete(
            @RequestBody(description = "Token IDs to delete")
            @NotNull final BulkOperationRequest request) {
        if (request.entityIds() == null || request.entityIds().isEmpty()) {
            throw new BadRequestException("No IDs provided in the request");
        }
        try (Stream<EnrollmentTokenDTO> stream = enrollmentTokenService.findByIds(request.entityIds())) {
            // we need to create the proper audit contexts for each delete event
            final List<Map<String, Object>> auditContexts = new ArrayList<>();
            final List<String> permittedIds =
                    stream.filter(dto -> isPermitted(CollectorsPermissions.FLEET_INSTANCE_ASSIGN, dto.fleetId()))
                            .peek(dto -> auditContexts.add(Map.of(
                                    "tokenId", Objects.requireNonNull(dto.id()),
                                    "name", dto.name(),
                                    "expiresAt", Objects.requireNonNullElse(dto.expiresAt(), "never"),
                                    "fleetId", dto.fleetId()
                                    )))
                            .map(EnrollmentTokenDTO::id)
                            .toList();
            final long deleted = enrollmentTokenService.deleteMany(permittedIds);

            // after deletion worked, send audit events for each bulk item
            final AuditActor auditActor = getAuditActor();
            auditContexts.forEach(context -> auditEventSender.success(auditActor, AuditEventTypes.OPAMP_ENROLLMENT_TOKEN_DELETE, context));

            return new BulkOperationResponse(Ints.saturatedCast(deleted), List.of());
        }
    }

    @Nonnull
    private AuditActor getAuditActor() {
        return AuditActor.user(getCurrentUser());
    }
}
