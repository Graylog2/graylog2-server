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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.audit.ViewsAuditEventTypes;
import org.graylog.plugins.views.search.export.ExportJobFactory;
import org.graylog.plugins.views.search.export.ExportJobService;
import org.graylog.plugins.views.search.export.MessagesRequest;
import org.graylog.plugins.views.search.export.ResultFormat;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Api(value = "Search/Messages", description = "Simple search returning (matching) messages only, as CSV.")
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

    @ApiOperation(value = "Create job to export a defined set of messages")
    @POST
    @AuditEvent(type = ViewsAuditEventTypes.EXPORT_JOB_CREATED)
    public String create(@ApiParam @Valid MessagesRequest rawrequest) {
        return exportJobService.save(exportJobFactory.fromMessagesRequest(rawrequest));
    }

    @ApiOperation(value = "Create job to export search result")
    @POST
    @Path("{searchId}")
    @AuditEvent(type = ViewsAuditEventTypes.EXPORT_JOB_CREATED)
    public String createForSearch(
            @ApiParam(value = "ID of an existing Search", name = "searchId") @PathParam("searchId") String searchId,
            @ApiParam(value = "Optional overrides") @Valid ResultFormat formatFromClient) {
        return exportJobService.save(exportJobFactory.forSearch(searchId, formatFromClient));
    }

    @ApiOperation(value = "Create job to export search type")
    @POST
    @Path("{searchId}/{searchTypeId}")
    @AuditEvent(type = ViewsAuditEventTypes.EXPORT_JOB_CREATED)
    public String createForSearchType(
            @ApiParam(value = "ID of an existing Search", name = "searchId") @PathParam("searchId") String searchId,
            @ApiParam(value = "ID of a Message Table contained in the Search", name = "searchTypeId") @PathParam("searchTypeId") String searchTypeId,
            @ApiParam(value = "Optional overrides") @Valid ResultFormat formatFromClient) {
        return exportJobService.save(exportJobFactory.forSearchType(searchId, searchTypeId, formatFromClient));
    }
}

