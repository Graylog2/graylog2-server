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
package org.graylog2.rest.resources.system.field_types;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesListService;
import org.graylog2.indexer.fieldtypes.mapping.FieldTypeMappingsService;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.rest.bulk.model.BulkOperationFailure;
import org.graylog2.rest.bulk.model.BulkOperationResponse;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldType;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.graylog2.audit.AuditEventTypes.FIELD_TYPE_MAPPING_CREATE;
import static org.graylog2.audit.AuditEventTypes.FIELD_TYPE_MAPPING_DELETE;
import static org.graylog2.audit.AuditEventTypes.INDEX_SET_UPDATE;

@RequiresAuthentication
@PublicCloudAPI
@Tag(name = "System/FieldTypes")
@Path("/system/indices/mappings")
@Produces(MediaType.APPLICATION_JSON)
public class FieldTypeMappingsResource extends RestResource {

    private final FieldTypeMappingsService fieldTypeMappingsService;
    private final IndexFieldTypesListService indexFieldTypesListService;
    private final IndexSetService indexSetService;
    private final AuditEventSender auditEventSender;

    @Inject
    public FieldTypeMappingsResource(final FieldTypeMappingsService fieldTypeMappingsService,
                                     final IndexFieldTypesListService indexFieldTypesListService,
                                     final IndexSetService indexSetService,
                                     final AuditEventSender auditEventSender) {
        this.fieldTypeMappingsService = fieldTypeMappingsService;
        this.indexFieldTypesListService = indexFieldTypesListService;
        this.indexSetService = indexSetService;
        this.auditEventSender = auditEventSender;
    }

    @GET
    @Path("/types")
    @Timed
    @Operation(summary = "Get list of all types valid inside the indexer")
    public Map<String, String> getAllFieldTypes() {
        return CustomFieldMappings.AVAILABLE_TYPES.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().description()));
    }

    @PUT
    @Timed
    @Operation(summary = "Change field type for certain index sets")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Unauthorized")
    })
    @AuditEvent(type = FIELD_TYPE_MAPPING_CREATE)
    public Map<String, IndexSetFieldType> changeFieldType(@Parameter(name = "request")
                                    @Valid
                                    @NotNull(message = "Request body is mandatory") final FieldTypeChangeRequest request) {
        checkPermissions(request.indexSetsIds(), RestPermissions.TYPE_MAPPINGS_CREATE);

        var customMapping = new CustomFieldMapping(request.fieldName(), request.type());
        fieldTypeMappingsService.changeFieldType(customMapping, request.indexSetsIds(), request.rotateImmediately());

        return newFieldTypes(request.indexSetsIds(), request.fieldName());
    }

    private Map<String, IndexSetFieldType> newFieldTypes(Set<String> indexSetIds, String fieldName) {
        final var newIndexFieldTypes = indexFieldTypesListService.getIndexSetFieldTypesList(indexSetIds, Set.of(fieldName));

        return newIndexFieldTypes.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue()
                                .stream()
                                .filter(fieldType -> fieldType.fieldName().equals(fieldName))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("Missing entry in field types list."))));
    }

    @PUT
    @Path("/set_profile")
    @Timed
    @Operation(summary = "Set field type profile for certain index sets")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "403", description = "Unauthorized")
    })
    @AuditEvent(type = INDEX_SET_UPDATE)
    public Response setProfile(@Parameter(name = "request")
                               @Valid
                               @NotNull(message = "Request body is mandatory") final FieldTypeProfileChangeRequest request) {
        checkPermissions(request.indexSetsIds(), RestPermissions.INDEXSETS_EDIT);
        fieldTypeMappingsService.setProfile(request.indexSetsIds(), request.profileId(), request.rotateImmediately());

        return Response.ok().build();
    }

    @PUT
    @Path("/remove_profile_from")
    @Timed
    @Operation(summary = "Remove field type profile from certain index sets")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "403", description = "Unauthorized")
    })
    @AuditEvent(type = INDEX_SET_UPDATE)
    public Response removeProfileFromIndexSets(@Parameter(name = "request")
                                               @Valid
                                               @NotNull(message = "Request body is mandatory") final FieldTypeProfileUnsetRequest request) {
        checkPermissions(request.indexSetsIds(), RestPermissions.INDEXSETS_EDIT);
        fieldTypeMappingsService.removeProfileFromIndexSets(request.indexSetsIds(), request.rotateImmediately());

        return Response.ok().build();
    }

    @PUT
    @Path("/bulk_set_profile")
    @Timed
    @Operation(summary = "Set field type profile for certain index sets, reporting the result per index set")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Unauthorized")
    })
    @NoAuditEvent("Each index set is audited individually")
    public Map<String, BulkOperationResponse> bulkSetProfile(@Parameter(name = "request")
                                                             @Valid
                                                             @NotNull(message = "Request body is mandatory") final FieldTypeProfileChangeRequest request) {
        return changeProfileOfPermitted(request.indexSetsIds(),
                Map.of("profile_id", request.profileId(), "rotate", request.rotateImmediately()),
                permittedIds -> fieldTypeMappingsService.bulkSetProfile(permittedIds, request.profileId(), request.rotateImmediately()));
    }

    @PUT
    @Path("/bulk_remove_profile")
    @Timed
    @Operation(summary = "Remove field type profile from certain index sets, reporting the result per index set")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Unauthorized")
    })
    @NoAuditEvent("Each index set is audited individually")
    public Map<String, BulkOperationResponse> bulkRemoveProfile(@Parameter(name = "request")
                                                                @Valid
                                                                @NotNull(message = "Request body is mandatory") final FieldTypeProfileUnsetRequest request) {
        return changeProfileOfPermitted(request.indexSetsIds(),
                Map.of("rotate", request.rotateImmediately()),
                permittedIds -> fieldTypeMappingsService.bulkRemoveProfile(permittedIds, request.rotateImmediately()));
    }

    /**
     * Denied index sets become per-set failures; a request with no editable index set gets 403.
     * The audit context carries the index set as {@code response_entity}, where the audit log formatter reads id and title.
     */
    private Map<String, BulkOperationResponse> changeProfileOfPermitted(final Set<String> indexSetsIds,
                                                                        final Map<String, Object> auditContext,
                                                                        final Function<Set<String>, Map<String, BulkOperationResponse>> change) {
        final AuditActor actor = AuditActor.user(getCurrentUser());
        final Set<String> permittedIds = indexSetsIds.stream()
                .filter(indexSetId -> isPermitted(RestPermissions.INDEXSETS_EDIT, indexSetId))
                .collect(Collectors.toSet());
        if (permittedIds.isEmpty()) {
            auditEventSender.failure(actor, INDEX_SET_UPDATE, ImmutableMap.<String, Object>builder()
                    .putAll(auditContext).put("index_sets", indexSetsIds).build());
            throw new ForbiddenException("Not authorized");
        }

        final Map<String, String> titles = indexSetService.findByIds(indexSetsIds)
                .stream()
                .collect(Collectors.toMap(IndexSetConfig::id, IndexSetConfig::title));
        final Map<String, BulkOperationResponse> result = new HashMap<>(change.apply(permittedIds));
        Sets.difference(indexSetsIds, permittedIds).forEach(indexSetId -> result.put(indexSetId,
                new BulkOperationResponse(0, List.of(new BulkOperationFailure(indexSetId, "Not authorized")))));

        result.forEach((indexSetId, response) -> {
            if (!titles.containsKey(indexSetId)) {
                return; // unknown index set: reported in the response, but not audited
            }
            final Map<String, Object> context = ImmutableMap.<String, Object>builder()
                    .putAll(auditContext)
                    .put("response_entity", Map.of("id", indexSetId, "title", titles.get(indexSetId)))
                    .build();
            if (response.successfullyPerformed() > 0) {
                auditEventSender.success(actor, INDEX_SET_UPDATE, context);
            } else {
                auditEventSender.failure(actor, INDEX_SET_UPDATE, context);
            }
        });
        return result;
    }

    @PUT
    @Path("/remove_mapping")
    @Timed
    @Operation(summary = "Remove custom field mapping for certain index sets")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Unauthorized")
    })
    @AuditEvent(type = FIELD_TYPE_MAPPING_DELETE)
    public Map<String, MappingRemovalResult> removeCustomMapping(@Parameter(name = "request")
                                                                  @Valid
                                                                  @NotNull(message = "Request body is mandatory") final CustomFieldMappingRemovalRequest request) {
        checkPermissions(request.indexSetsIds(), RestPermissions.TYPE_MAPPINGS_DELETE);

        final var result = fieldTypeMappingsService.removeCustomMappingForFields(request.fieldNames(), request.indexSetsIds(), request.rotateImmediately());
        final var newTypes = this.indexFieldTypesListService.getIndexSetFieldTypesList(request.indexSetsIds(), request.fieldNames());
        return result
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, value -> new MappingRemovalResult(
                        value.getValue().successfullyPerformed(),
                        value.getValue().failures(),
                        value.getValue().errors(),
                        newTypes.get(value.getKey())
                )));
    }

    public record MappingRemovalResult(@JsonProperty("successfully_performed") int successfullyPerformed,
                                       @JsonProperty("failures") List<BulkOperationFailure> failures,
                                       @JsonProperty("errors") List<String> errors,
                                       @JsonProperty("succeeded") List<IndexSetFieldType> succeeded) {}

    private void checkPermissions(final Set<String> indexSetsIds, final String permission) {
        indexSetsIds.forEach(indexSetId -> checkPermission(permission, indexSetId));
    }
}
