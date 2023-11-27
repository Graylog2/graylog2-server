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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.indexer.fieldtypes.mapping.FieldTypeMappingsService;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.rest.bulk.model.BulkOperationResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.audit.AuditEventTypes.FIELD_TYPE_MAPPING_CREATE;
import static org.graylog2.audit.AuditEventTypes.FIELD_TYPE_MAPPING_DELETE;
import static org.graylog2.indexer.fieldtypes.mapping.FieldTypeMappingsService.BLACKLISTED_FIELDS;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "System/FieldTypes", tags = {CLOUD_VISIBLE})
@Path("/system/indices/mappings")
@Produces(MediaType.APPLICATION_JSON)
public class FieldTypeMappingsResource extends RestResource {

    private final FieldTypeMappingsService fieldTypeMappingsService;

    @Inject
    public FieldTypeMappingsResource(final FieldTypeMappingsService fieldTypeMappingsService) {
        this.fieldTypeMappingsService = fieldTypeMappingsService;
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
    public Response changeFieldType(@ApiParam(name = "request")
                                    @Valid
                                    @NotNull(message = "Request body is mandatory") final FieldTypeChangeRequest request) {
        checkPermissionsForCreation(request.indexSetsIds());
        checkFieldIsAllowedToBeChanged(request.fieldName());

        var type = CustomFieldMappings.AVAILABLE_TYPES.get(request.type());
        if (type == null) {
            throw new BadRequestException("Invalid type provided: " + request.type() + " - available types: " + CustomFieldMappings.AVAILABLE_TYPES.keySet());
        }

        var customMapping = new CustomFieldMapping(request.fieldName(), request.type());
        fieldTypeMappingsService.changeFieldType(customMapping, request.indexSetsIds(), request.rotateImmediately());

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
    public Map<String, BulkOperationResponse> removeCustomMapping(@ApiParam(name = "request")
                                        @Valid
                                        @NotNull(message = "Request body is mandatory") final CustomFieldMappingRemovalRequest request) {
        checkPermissionsForCreation(request.indexSetsIds());

        return fieldTypeMappingsService.removeCustomMappingForFields(request.fieldNames(), request.indexSetsIds(), request.rotateImmediately());
    }

    private void checkFieldIsAllowedToBeChanged(String fieldName) {
        if (BLACKLISTED_FIELDS.contains(fieldName)) {
            throw new BadRequestException("Unable to change field type of " + fieldName + ", not allowed to change type of these fields: " + BLACKLISTED_FIELDS);
        }
    }


    private void checkPermissionsForCreation(final Set<String> indexSetsIds) {
        indexSetsIds.forEach(indexSetId -> checkPermission(RestPermissions.TYPE_MAPPINGS_CREATE, indexSetId));
    }
}
