package org.graylog.plugins.views.search.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.export.ExportJobFactory;
import org.graylog.plugins.views.search.export.ExportJobService;
import org.graylog.plugins.views.search.export.MessagesRequest;
import org.graylog.plugins.views.search.export.ResultFormat;
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

    @POST
    public String create(@ApiParam @Valid MessagesRequest rawrequest) {
        return exportJobService.save(exportJobFactory.fromMessagesRequest(rawrequest));
    }

    @ApiOperation(value = "Create job to export search result")
    @POST
    @Path("{searchId}")
    public String createForSearch(
            @ApiParam(value = "ID of an existing Search", name = "searchId") @PathParam("searchId") String searchId,
            @ApiParam(value = "Optional overrides") @Valid ResultFormat formatFromClient) {
        return exportJobService.save(exportJobFactory.forSearch(searchId, formatFromClient));
    }

    @ApiOperation(value = "Create job to export search type")
    @POST
    @Path("{searchId}/{searchTypeId}")
    public String createForSearchType(
            @ApiParam(value = "ID of an existing Search", name = "searchId") @PathParam("searchId") String searchId,
            @ApiParam(value = "ID of a Message Table contained in the Search", name = "searchTypeId") @PathParam("searchTypeId") String searchTypeId,
            @ApiParam(value = "Optional overrides") @Valid ResultFormat formatFromClient) {
        return exportJobService.save(exportJobFactory.forSearchType(searchId, searchTypeId, formatFromClient));
    }
}

