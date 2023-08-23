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
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.MongoIndexSetService;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.Set;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "System/FieldTypes", tags = {CLOUD_VISIBLE})
@Path("/system/indices/field_types_change")
@Produces(MediaType.APPLICATION_JSON)
public class FieldTypeChangeResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(FieldTypeChangeResource.class);

    private final IndexSetService indexSetService;
    private final MongoIndexSet.Factory mongoIndexSetFactory;
    private final MongoIndexSetService mongoIndexSetService;

    @Inject
    public FieldTypeChangeResource(final IndexSetService indexSetService,
                                   final MongoIndexSet.Factory mongoIndexSetFactory,
                                   final MongoIndexSetService mongoIndexSetService) {
        this.indexSetService = indexSetService;
        this.mongoIndexSetFactory = mongoIndexSetFactory;
        this.mongoIndexSetService = mongoIndexSetService;
    }

    @PUT
    @Path("{field_name}")
    @Timed
    @ApiOperation(value = "Change field type for certain index sets")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Unauthorized")
    })
    @NoAuditEvent("No audit for field type changes")
    public Response changeFieldType(@ApiParam(name = "index_sets_ids", required = true)
                                    @QueryParam("index_sets_ids") Set<String> indexSetsIds,
                                    @ApiParam(name = "field_name", required = true)
                                    @PathParam("field_name") String fieldName,
                                    @ApiParam(name = "new_type", required = true)
                                    @QueryParam("new_type") String newType,
                                    @ApiParam(name = "rotate_immediately", defaultValue = "false")
                                    @QueryParam("rotate_immediately") boolean rotateImmediately
    ) {
        checkPermissions(indexSetsIds);

        //TODO: validation of fields

        CustomFieldMappings.CustomFieldMapping customMapping = new CustomFieldMappings.CustomFieldMapping(fieldName, newType);

        for (String indexSetId : indexSetsIds) {
            try {
                final Optional<IndexSetConfig> indexSetConfigOpt = indexSetService.get(indexSetId);
                if (indexSetConfigOpt.isPresent()) {
                    final IndexSetConfig indexSetConfig = indexSetConfigOpt.get();
                    final IndexSetConfig updatedIndexSetConfig = storeMapping(customMapping, indexSetConfig);

                    if (rotateImmediately) {
                        cycleIndexSet(updatedIndexSetConfig);
                    }
                }
            } catch (Exception ex) {
                LOG.error("Failed to update field type in index set : " + indexSetId, ex);
            }
        }

        return Response.ok().build();
    }

    private IndexSetConfig storeMapping(final CustomFieldMappings.CustomFieldMapping customMapping,
                                        final IndexSetConfig indexSetConfig) {
        final IndexSetConfig.Builder builder = indexSetConfig.toBuilder();
        final CustomFieldMappings previousCustomFieldMappings = indexSetConfig.customFieldMappings();
        if (previousCustomFieldMappings == null) {
            builder.customFieldMappings(new CustomFieldMappings(customMapping.toSet()));
        } else {
            builder.customFieldMappings(previousCustomFieldMappings.modifiedWith(customMapping.toSet()));
        }

        return mongoIndexSetService.save(builder.build());
    }

    private void cycleIndexSet(IndexSetConfig indexSetConfig) {
        final MongoIndexSet mongoIndexSet = mongoIndexSetFactory.create(indexSetConfig);
        mongoIndexSet.cycle();
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
