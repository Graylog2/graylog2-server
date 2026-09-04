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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.audit.ViewsAuditEventTypes;
import org.graylog.plugins.views.search.export.ExportJobFactory;
import org.graylog.plugins.views.search.export.ExportJobService;
import org.graylog.plugins.views.search.export.MessagesRequest;
import org.graylog.plugins.views.search.export.ResultFormat;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;


@PublicCloudAPI
@Tag(name = "Search/Export", description = "Creating/Managing Export Jobs.")
@Path("/views/export")
@RequiresAuthentication
public class ExportJobsResource extends RestResource {
    private final ExportJobService exportJobService;
    private final ExportJobFactory exportJobFactory;

    @Inject
    public ExportJobsResource(ExportJobService exportJobService,
                              ExportJobFactory exportJobFactory) {
        this.exportJobService = exportJobService;
        this.exportJobFactory = exportJobFactory;
    }

    @Operation(summary = "Create job to export a defined set of messages")
    @POST
    @AuditEvent(type = ViewsAuditEventTypes.EXPORT_JOB_CREATED)
    public String create(@RequestBody(required = true) @Valid MessagesRequest rawrequest) {
        return exportJobService.save(exportJobFactory.fromMessagesRequest(rawrequest));
    }

    @Operation(summary = "Create job to export search result")
    @POST
    @Path("{searchId}")
    @AuditEvent(type = ViewsAuditEventTypes.EXPORT_JOB_CREATED)
    public String createForSearch(
            @Parameter(description = "ID of an existing Search", name = "searchId") @PathParam("searchId") String searchId,
            @Parameter(name = "Optional overrides") @Valid ResultFormat formatFromClient) {
        return exportJobService.save(exportJobFactory.forSearch(searchId, formatFromClient));
    }

    @Operation(summary = "Create job to export search type")
    @POST
    @Path("{searchId}/{searchTypeId}")
    @AuditEvent(type = ViewsAuditEventTypes.EXPORT_JOB_CREATED)
    public String createForSearchType(
            @Parameter(description = "ID of an existing Search", name = "searchId") @PathParam("searchId") String searchId,
            @Parameter(description = "ID of a Message Table contained in the Search", name = "searchTypeId") @PathParam("searchTypeId") String searchTypeId,
            @Parameter(name = "Optional overrides") @Valid ResultFormat formatFromClient) {
        return exportJobService.save(exportJobFactory.forSearchType(searchId, searchTypeId, formatFromClient));
    }
}

