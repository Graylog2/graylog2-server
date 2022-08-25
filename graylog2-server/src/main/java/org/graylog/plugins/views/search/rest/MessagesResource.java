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
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.validation.QueryValidationService;
import org.graylog.plugins.views.search.validation.ValidationMessage;
import org.graylog.plugins.views.search.validation.ValidationRequest;
import org.graylog.plugins.views.search.validation.ValidationResponse;
import org.graylog.plugins.views.search.validation.ValidationStatus;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.shared.rest.resources.RestResource;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Search/Messages", description = "Simple search returning (matching) messages only, as CSV.", tags = {CLOUD_VISIBLE})
@Path("/views/search/messages")
@RequiresAuthentication
public class MessagesResource extends RestResource implements PluginRestResource {
    private static final DateTimeZone FALLBACK_TIME_ZONE = DateTimeZone.UTC;
    private final CommandFactory commandFactory;
    private final SearchDomain searchDomain;
    private final SearchExecutionGuard executionGuard;
    private final ExportJobService exportJobService;
    private final QueryValidationService queryValidationService;

    //allow mocking
    Function<Consumer<Consumer<SimpleMessageChunk>>, ChunkedOutput<SimpleMessageChunk>> asyncRunner = ChunkedRunner::runAsync;
    Function<AuditContext, MessagesExporter> messagesExporterFactory;

    @Inject
    public MessagesResource(
            MessagesExporter exporter,
            CommandFactory commandFactory,
            SearchDomain searchDomain,
            SearchExecutionGuard executionGuard,
            @SuppressWarnings("UnstableApiUsage") EventBus eventBus,
            ExportJobService exportJobService, QueryValidationService queryValidationService) {
        this.commandFactory = commandFactory;
        this.searchDomain = searchDomain;
        this.executionGuard = executionGuard;
        this.exportJobService = exportJobService;
        this.queryValidationService = queryValidationService;
        this.messagesExporterFactory = context -> new AuditingMessagesExporter(context, eventBus, exporter);
    }

    @ApiOperation(
            value = "Export messages as CSV",
            notes = "Use this endpoint, if you want to configure export parameters freely instead of relying on an existing Search"
    )
    @POST
    @Produces(MoreMediaTypes.TEXT_CSV)
    @NoAuditEvent("Has custom audit events")
    public ChunkedOutput<SimpleMessageChunk> retrieve(@ApiParam @Valid MessagesRequest rawrequest,
                                                      @Context SearchUser searchUser) {

        final MessagesRequest request = fillInIfNecessary(rawrequest, searchUser);

        final ValidationRequest.Builder validationReq = ValidationRequest.builder();
        Optional.ofNullable(rawrequest.queryString()).ifPresent(validationReq::query);
        Optional.ofNullable(rawrequest.timeRange()).ifPresent(validationReq::timerange);
        Optional.ofNullable(rawrequest.streams()).ifPresent(validationReq::streams);
        final ValidationResponse validationResponse = queryValidationService.validate(validationReq.build());
        if (validationResponse.status().equals(ValidationStatus.ERROR)) {
            validationResponse.explanations().stream().findFirst().map(ValidationMessage::errorMessage).ifPresent(message -> {
                throw new BadRequestException("Request validation failed: " + message);
            });
        }

        executionGuard.checkUserIsPermittedToSeeStreams(request.streams(), searchUser::canReadStream);

         ExportMessagesCommand command = commandFactory.buildFromRequest(request);

        return asyncRunner.apply(chunkConsumer -> exporter().export(command, chunkConsumer));
    }

    private MessagesRequest fillInIfNecessary(MessagesRequest requestFromClient, SearchUser searchUser) {
        MessagesRequest request = requestFromClient != null ? requestFromClient : MessagesRequest.withDefaults();

        if (request.streams().isEmpty()) {
            request = request.withStreams(searchUser.streams().loadAll());
        }

        if (!request.timeZone().isPresent()) {
            request = request.withTimeZone(searchUser.timeZone().orElse(FALLBACK_TIME_ZONE));
        }

        return request;
    }

    private ResultFormat fillInIfNecessary(ResultFormat resultFormat, SearchUser searchUser) {
        return resultFormat.timeZone().isPresent()
                ? resultFormat
                : resultFormat.withTimeZone(searchUser.timeZone().orElse(FALLBACK_TIME_ZONE));
    }

    @ApiOperation(value = "Export a search result as CSV")
    @POST
    @Path("{searchId}")
    @Produces(MoreMediaTypes.TEXT_CSV)
    @NoAuditEvent("Has custom audit events")
    public ChunkedOutput<SimpleMessageChunk> retrieveForSearch(
            @ApiParam(value = "ID of an existing Search", name = "searchId") @PathParam("searchId") String searchId,
            @ApiParam(value = "Optional overrides") @Valid ResultFormat formatFromClient,
            @Context SearchUser searchUser) {
        ResultFormat format = fillInIfNecessary(emptyIfNull(formatFromClient), searchUser);

        Search search = loadSearch(searchId, format.executionState(), searchUser);

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
            @ApiParam(value = "Optional overrides") @Valid ResultFormat formatFromClient,
            @Context SearchUser searchUser) {
        ResultFormat format = fillInIfNecessary(emptyIfNull(formatFromClient), searchUser);

        Search search = loadSearch(searchId, format.executionState(), searchUser);

        ExportMessagesCommand command = commandFactory.buildWithMessageList(search, searchTypeId, format);

        return asyncRunner.apply(chunkConsumer -> exporter(searchId, searchTypeId).export(command, chunkConsumer));
    }

    @ApiOperation("Retrieve results for export job")
    @GET
    @Path("job/{exportJobId}/{filename}")
    public ChunkedOutput<SimpleMessageChunk> retrieveForExportJob(@ApiParam(value = "ID of an existing export job", name = "exportJobId")
                                                                  @PathParam("exportJobId") String exportJobId,
                                                                  @ApiParam(value = "Resulting filename", name = "filename")
                                                                  @PathParam("filename") String filename,
                                                                  @Context SearchUser searchUser) {
        final ExportJob exportJob = exportJobService.get(exportJobId)
                .orElseThrow(() -> new NotFoundException("Unable to find export job with id <" + exportJobId + ">!"));

        return outputFor(exportJob, searchUser);
    }

    private ChunkedOutput<SimpleMessageChunk> outputFor(ExportJob exportJob, SearchUser searchUser) {
        if (exportJob instanceof MessagesRequestExportJob) {
            final MessagesRequest messagesRequest = ((MessagesRequestExportJob) exportJob).messagesRequest();
            return this.retrieve(messagesRequest, searchUser);
        }

        if (exportJob instanceof SearchExportJob) {
            final SearchExportJob searchExportJob = (SearchExportJob) exportJob;
            return this.retrieveForSearch(searchExportJob.searchId(), searchExportJob.resultFormat(), searchUser);
        }

        if (exportJob instanceof SearchTypeExportJob) {
            final SearchTypeExportJob searchTypeExportJob = (SearchTypeExportJob) exportJob;
            return this.retrieveForSearchType(searchTypeExportJob.searchId(), searchTypeExportJob.searchTypeId(), searchTypeExportJob.resultFormat(), searchUser);
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

    private Search loadSearch(String searchId, ExecutionState executionState, SearchUser searchUser) {
        Search search = searchDomain.getForUser(searchId, searchUser)
                .orElseThrow(() -> new NotFoundException("Search with id " + searchId + " does not exist"));

        search = search.addStreamsToQueriesWithoutStreams(() -> searchUser.streams().loadAll());

        search = search.applyExecutionState(executionState);

        executionGuard.check(search, searchUser::canReadStream);

        return search;
    }
}
