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
package org.graylog.plugins.views.search.rest;

import com.google.common.collect.ImmutableSet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerPeriodical;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.Set;

import static org.graylog2.audit.AuditEventTypes.FIELD_TYPE_POLLING_TRIGGERED;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "FieldTypes", tags = {CLOUD_VISIBLE})
@Path("/views/fields")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class FieldTypesResource extends RestResource implements PluginRestResource {
    private final MappedFieldTypesService mappedFieldTypesService;
    private final IndexFieldTypePollerPeriodical fieldTypePoller;

    @Inject
    public FieldTypesResource(MappedFieldTypesService mappedFieldTypesService, IndexFieldTypePollerPeriodical fieldTypePoller) {
        this.mappedFieldTypesService = mappedFieldTypesService;
        this.fieldTypePoller = fieldTypePoller;
    }

    @GET
    @ApiOperation(value = "Retrieve the list of all fields present in the system")
    public Set<MappedFieldTypeDTO> allFieldTypes(@Context SearchUser searchUser) {
        final ImmutableSet<String> streams = searchUser.streams().loadAllMessageStreams();
        return mappedFieldTypesService.fieldTypesByStreamIds(streams, RelativeRange.allTime());
    }

    @POST
    @ApiOperation(value = "Retrieve the field list of a given set of streams")
    @NoAuditEvent("This is not changing any data")
    public Set<MappedFieldTypeDTO> byStreams(@ApiParam(name = "JSON body", required = true)
                                             @Valid @NotNull FieldTypesForStreamsRequest request,
                                             @Context SearchUser searchUser) {
        final ImmutableSet<String> streams = searchUser.streams().readableOrAllIfEmpty(request.streams());
        return mappedFieldTypesService.fieldTypesByStreamIds(streams, request.timerange().orElse(RelativeRange.allTime()));
    }

    @POST
    @ApiOperation(value = "Trigger a full refresh of field types")
    @Path("/poll")
    @RequiresPermissions("*")
    @AuditEvent(type = FIELD_TYPE_POLLING_TRIGGERED)
    public Response triggerFieldTypePolling() {
        fieldTypePoller.triggerFullRefresh();

        return Response.noContent().build();
    }
}
