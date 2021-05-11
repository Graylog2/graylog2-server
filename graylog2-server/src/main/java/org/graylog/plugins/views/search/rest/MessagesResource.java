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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.glassfish.jersey.server.ChunkedOutput;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.SearchExecutionGuard;
import org.graylog.plugins.views.search.export.AuditContext;
import org.graylog.plugins.views.search.export.AuditingMessagesExporter;
import org.graylog.plugins.views.search.export.ChunkedRunner;
import org.graylog.plugins.views.search.export.CommandFactory;
import org.graylog.plugins.views.search.export.ExportJob;
import org.graylog.plugins.views.search.export.ExportJobService;
import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog.plugins.views.search.export.MessagesExporter;
import org.graylog.plugins.views.search.export.MessagesRequest;
import org.graylog.plugins.views.search.export.MessagesRequestExportJob;
import org.graylog.plugins.views.search.export.ResultFormat;
import org.graylog.plugins.views.search.export.SearchExportJob;
import org.graylog.plugins.views.search.export.SearchTypeExportJob;
import org.graylog.plugins.views.search.export.SimpleMessageChunk;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@Api(value = "Search/Messages", description = "Simple search returning (matching) messages only, as CSV.")
@Path("/views/search/messages")
@RequiresAuthentication
public class MessagesResource extends RestResource implements PluginRestResource {

    private final CommandFactory commandFactory;
    private final SearchDomain searchDomain;
    private final SearchExecutionGuard executionGuard;
    private final PermittedStreams permittedStreams;
    private final ObjectMapper objectMapper;
    private final ExportJobService exportJobService;

    //allow mocking
    Function<Consumer<Consumer<SimpleMessageChunk>>, ChunkedOutput<SimpleMessageChunk>> asyncRunner = ChunkedRunner::runAsync;
    Function<AuditContext, MessagesExporter> messagesExporterFactory;

    @Inject
    public MessagesResource(
            MessagesExporter exporter,
            CommandFactory commandFactory,
            SearchDomain searchDomain,
            SearchExecutionGuard executionGuard,
            PermittedStreams permittedStreams,
            ObjectMapper objectMapper,
            @SuppressWarnings("UnstableApiUsage") EventBus eventBus,
            ExportJobService exportJobService) {
        this.commandFactory = commandFactory;
        this.searchDomain = searchDomain;
        this.executionGuard = executionGuard;
        this.permittedStreams = permittedStreams;
        this.objectMapper = objectMapper;
        this.exportJobService = exportJobService;
        this.messagesExporterFactory = context -> new AuditingMessagesExporter(context, eventBus, exporter);
    }

    @ApiOperation(
            value = "Export messages as CSV",
            notes = "Use this endpoint, if you want to configure export parameters freely instead of relying on an existing Search"
    )
    @POST
    @Produces(MoreMediaTypes.TEXT_CSV)
    @NoAuditEvent("Has custom audit events")
    public ChunkedOutput<SimpleMessageChunk> retrieve(@ApiParam @Valid MessagesRequest rawrequest) {
        final MessagesRequest request = fillInIfNecessary(rawrequest);

        executionGuard.checkUserIsPermittedToSeeStreams(request.streams(), this::hasStreamReadPermission);

        ExportMessagesCommand command = commandFactory.buildFromRequest(request);

        return asyncRunner.apply(chunkConsumer -> exporter().export(command, chunkConsumer));
    }

    private MessagesRequest fillInIfNecessary(MessagesRequest requestFromClient) {
        MessagesRequest request = requestFromClient != null ? requestFromClient : MessagesRequest.withDefaults();

        if (request.streams().isEmpty()) {
            request = request.toBuilder().streams(loadAllAllowedStreamsForUser()).build();
        }
        return request;
    }

    @ApiOperation(value = "Export a search result as CSV")
    @POST
    @Path("{searchId}")
    @Produces(MoreMediaTypes.TEXT_CSV)
    @NoAuditEvent("Has custom audit events")
    public ChunkedOutput<SimpleMessageChunk> retrieveForSearch(
            @ApiParam(value = "ID of an existing Search", name = "searchId") @PathParam("searchId") String searchId,
            @ApiParam(value = "Optional overrides") @Valid ResultFormat formatFromClient) {
        ResultFormat format = emptyIfNull(formatFromClient);

        Search search = loadSearch(searchId, format.executionState());

        ExportMessagesCommand command = commandFactory.buildWithSearchOnly(search, format);

        return asyncRunner.apply(chunkConsumer -> exporter(searchId).export(command, chunkConsumer));
    }

    @ApiOperation(value = "Export a message table as CSV")
    @POST
    @Path("{searchId}/{searchTypeId}")
    @NoAuditEvent("Has custom audit events")
    public ChunkedOutput<SimpleMessageChunk> retrieveForSearchType(
            @ApiParam(value = "ID of an existing Search", name = "searchId") @PathParam("searchId") String searchId,
            @ApiParam(value = "ID of a Message Table contained in the Search", name = "searchTypeId") @PathParam("searchTypeId") String searchTypeId,
            @ApiParam(value = "Optional overrides") @Valid ResultFormat formatFromClient) {
        ResultFormat format = emptyIfNull(formatFromClient);

        Search search = loadSearch(searchId, format.executionState());

        ExportMessagesCommand command = commandFactory.buildWithMessageList(search, searchTypeId, format);

        return asyncRunner.apply(chunkConsumer -> exporter(searchId, searchTypeId).export(command, chunkConsumer));
    }

    @ApiOperation("Retrieve results for export job")
    @GET
    @Path("job/{exportJobId}/{filename:.*}")
    public Response retrieveForExportJob(@ApiParam(value = "ID of an existing export job", name = "exportJobId")
                                         @PathParam("exportJobId") String exportJobId) throws UnsupportedEncodingException {
        final ExportJob exportJob = exportJobService.get(exportJobId)
                .orElseThrow(() -> new NotFoundException("Unable to find export job with id <" + exportJobId + ">!"));

        final ChunkedOutput<SimpleMessageChunk> output = outputFor(exportJob);

        //final String encodedFilename = URLEncoder.encode(filename + "." + extension, StandardCharsets.UTF_8.toString());

        return Response.ok()
                .entity(output)
                //.header("Content-Disposition", "attachment; filename=\"" + encodedFilename +"\"")
                .build();
    }

    private ChunkedOutput<SimpleMessageChunk> outputFor(ExportJob exportJob) {
        if (exportJob instanceof MessagesRequestExportJob) {
            final MessagesRequest messagesRequest = ((MessagesRequestExportJob) exportJob).messagesRequest();
            return this.retrieve(messagesRequest);
        }

        if (exportJob instanceof SearchExportJob) {
            final ResultFormat resultFormat = ((SearchExportJob) exportJob).resultFormat();
            return this.retrieveForSearch(((SearchExportJob) exportJob).searchId(), resultFormat);
        }

        if (exportJob instanceof SearchTypeExportJob) {
            final ResultFormat resultFormat = ((SearchTypeExportJob) exportJob).resultFormat();
            return this.retrieveForSearchType(((SearchTypeExportJob) exportJob).searchId(), ((SearchTypeExportJob) exportJob).searchTypeId(), resultFormat);
        }

        throw new IllegalStateException("Invalid type of export job: " + exportJob.getClass());
    }

    private MessagesExporter exporter() {
        return exporter(null, null);
    }

    private MessagesExporter exporter(String searchId) {
        return exporter(searchId, null);
    }

    private MessagesExporter exporter(String searchId, String searchTypeId) {
        return messagesExporterFactory.apply(new AuditContext(userName(), searchId, searchTypeId));
    }

    private String userName() {
        return Objects.requireNonNull(getCurrentUser()).getName();
    }

    private ResultFormat emptyIfNull(ResultFormat format) {
        return format == null ? ResultFormat.empty() : format;
    }

    private Search loadSearch(String searchId, Map<String, Object> executionState) {
        Search search = searchDomain.getForUser(searchId, getCurrentUser(), this::hasViewReadPermission)
                .orElseThrow(() -> new NotFoundException("Search with id " + searchId + " does not exist"));

        search = search.addStreamsToQueriesWithoutStreams(this::loadAllAllowedStreamsForUser);

        search = search.applyExecutionState(objectMapper, executionState);

        executionGuard.check(search, this::hasStreamReadPermission);

        return search;
    }

    private boolean hasViewReadPermission(ViewDTO view) {
        return isPermitted(ViewsRestPermissions.VIEW_READ, view.id());
    }

    private ImmutableSet<String> loadAllAllowedStreamsForUser() {
        return permittedStreams.load(this::hasStreamReadPermission);
    }

    private boolean hasStreamReadPermission(String streamId) {
        return isPermitted(RestPermissions.STREAMS_READ, streamId);
    }
}
