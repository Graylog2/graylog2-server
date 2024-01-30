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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesListService;
import org.graylog2.indexer.fieldtypes.mapping.FieldTypeMappingsService;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldType;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.audit.AuditEventTypes.FIELD_TYPE_MAPPING_CREATE;
import static org.graylog2.audit.AuditEventTypes.FIELD_TYPE_MAPPING_DELETE;
import static org.graylog2.audit.AuditEventTypes.INDEX_SET_UPDATE;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "System/FieldTypes", tags = {CLOUD_VISIBLE})
@Path("/system/indices/mappings")
@Produces(MediaType.APPLICATION_JSON)
public class FieldTypeMappingsResource extends RestResource {

    private final FieldTypeMappingsService fieldTypeMappingsService;
    private final IndexFieldTypesListService indexFieldTypesListService;

    @Inject
    public FieldTypeMappingsResource(final FieldTypeMappingsService fieldTypeMappingsService, final IndexFieldTypesListService indexFieldTypesListService) {
        this.fieldTypeMappingsService = fieldTypeMappingsService;
        this.indexFieldTypesListService = indexFieldTypesListService;
    }

    @GET
    @Path("/types")
    @Timed
    @ApiOperation(value = "Get list of all types valid inside the indexer")
    public Map<String, String> getAllFieldTypes() {
        return CustomFieldMappings.AVAILABLE_TYPES.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().description()));
    }

    @PUT
    @Timed
    @ApiOperation(value = "Change field type for certain index sets")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Unauthorized")
    })
    @AuditEvent(type = FIELD_TYPE_MAPPING_CREATE)
    public Map<String, IndexSetFieldType> changeFieldType(@ApiParam(name = "request")
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
    @ApiOperation(value = "Set field type profile for certain index sets")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Unauthorized")
    })
    @AuditEvent(type = INDEX_SET_UPDATE)
    public Response setProfile(@ApiParam(name = "request")
                               @Valid
                               @NotNull(message = "Request body is mandatory") final FieldTypeProfileChangeRequest request) {
        checkPermissions(request.indexSetsIds(), RestPermissions.INDEXSETS_EDIT);
        fieldTypeMappingsService.setProfile(request.indexSetsIds(), request.profileId(), request.rotateImmediately());

        return Response.ok().build();
    }

    @PUT
    @Path("/remove_profile_from")
    @Timed
    @ApiOperation(value = "Remove field type profile from certain index sets")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Unauthorized")
    })
    @AuditEvent(type = INDEX_SET_UPDATE)
    public Response removeProfileFromIndexSets(@ApiParam(name = "request")
                                               @Valid
                                               @NotNull(message = "Request body is mandatory") final FieldTypeProfileUnsetRequest request) {
        checkPermissions(request.indexSetsIds(), RestPermissions.INDEXSETS_EDIT);
        fieldTypeMappingsService.removeProfileFromIndexSets(request.indexSetsIds(), request.rotateImmediately());

        return Response.ok().build();
    }

    @PUT
    @Path("/remove_mapping")
    @Timed
    @ApiOperation(value = "Remove custom field mapping for certain index sets")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Unauthorized")
    })
    @AuditEvent(type = FIELD_TYPE_MAPPING_DELETE)
    public Map<String, List<IndexSetFieldType>> removeCustomMapping(@ApiParam(name = "request")
                                                                  @Valid
                                                                  @NotNull(message = "Request body is mandatory") final CustomFieldMappingRemovalRequest request) {
        checkPermissions(request.indexSetsIds(), RestPermissions.TYPE_MAPPINGS_DELETE);

        fieldTypeMappingsService.removeCustomMappingForFields(request.fieldNames(), request.indexSetsIds(), request.rotateImmediately());
        return this.indexFieldTypesListService.getIndexSetFieldTypesList(request.indexSetsIds(), request.fieldNames());
    }

    private void checkPermissions(final Set<String> indexSetsIds, final String permission) {
        indexSetsIds.forEach(indexSetId -> checkPermission(permission, indexSetId));
    }
}
