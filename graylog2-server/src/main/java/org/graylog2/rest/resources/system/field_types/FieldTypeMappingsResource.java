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
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.fieldtypes.FieldTypeMapper;
import org.graylog2.indexer.fieldtypes.mapping.FieldTypeMappingsService;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

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

    @PUT
    @Timed
    @ApiOperation(value = "Change field type for certain index sets")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Unauthorized")
    })
    @NoAuditEvent("No audit for field type changes")
    public Response changeFieldType(@ApiParam(name = "request")
                                    @Valid
                                    @NotNull(message = "Request body is mandatory") final FieldTypeChangeRequest request
    ) {
        checkPermissions(request.indexSetsIds());

        //TODO: more complex validation of request
        if (!FieldTypeMapper.TYPE_MAP.containsKey(request.newType())) {
            throw new IllegalArgumentException("Invalid type provided : " + request.newType());
        }


        CustomFieldMapping customMapping = new CustomFieldMapping(request.fieldName(), request.newType());
        fieldTypeMappingsService.changeFieldType(customMapping, request.indexSetsIds(), request.rotateImmediately());

        return Response.ok().build();
    }



    private void checkPermissions(final Set<String> indexSetsIds) {
//        TODO:
//        Role permission check
//        final boolean hasProperRole = getSubject().hasRole("Template manager or smth like that");
//        if (!hasProperRole) {
//            throw new ForbiddenException("Not authorized ...");
//        }

        //or

//        TODO:
//        Individual permissions per index set
//        for (String indexSetId : indexSetsIds) {
//        what permission is needed?
//        do we want a partial change if permitted for some index sets?
//        checkPermission(RestPermissions.INDEXSETS_EDIT, indexSetId);
//        }
    }
}
