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
package org.graylog.plugins.views.search.rest.export;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.searchtypes.export.ExportTabularResultResponse;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.rest.RestTools;
import org.graylog2.shared.rest.resources.RestResource;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Search/Pivot/Export", tags = {CLOUD_VISIBLE})
@Path("/views/search/pivot/export")
@RequiresAuthentication
@Consumes(MediaType.APPLICATION_JSON)
public class AggregationWidgetExportResource extends RestResource {

    @ApiOperation(value = "Export widget data")
    @POST
    @NoAuditEvent("Exporting widget data does not need audit event")
    @Produces({MoreMediaTypes.TEXT_CSV,
            MediaType.APPLICATION_JSON,
            MoreMediaTypes.APPLICATION_YAML,
            MediaType.APPLICATION_XML,
            MoreMediaTypes.APPLICATION_XLS})
    @Path("/{filename}")
    public Response exportData(@ApiParam @Valid PivotResult pivotResult,
                               @HeaderParam("Accept") String mediaType,
                               @ApiParam("filename") @PathParam("filename") String filename) {
        return RestTools.respondWithFile(
                        filename,
                        ExportTabularResultResponse.fromPivotResult(pivotResult),
                        MediaType.valueOf(mediaType))
                .build();
    }
}
