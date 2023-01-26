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
package org.graylog.events.rest;


import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.events.audit.EventsAuditEventTypes;
import org.graylog.events.context.EventDefinitionContextService;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventDefinitionHandler;
import org.graylog.events.processor.EventProcessorEngine;
import org.graylog.events.processor.EventProcessorException;
import org.graylog.events.processor.EventProcessorParameters;
import org.graylog.events.processor.EventProcessorParametersWithTimerange;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityService;
import org.graylog.security.UserContext;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.rest.bulk.AuditParams;
import org.graylog2.rest.bulk.BulkExecutor;
import org.graylog2.rest.bulk.SequentialBulkExecutor;
import org.graylog2.rest.bulk.model.BulkOperationRequest;
import org.graylog2.rest.bulk.model.BulkOperationResponse;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Events/Definitions", description = "Event definition management", tags = {CLOUD_VISIBLE})
@Path("/events/definitions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class EventDefinitionsResource extends RestResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(EventDefinitionsResource.class);

    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create("_id", SearchQueryField.Type.OBJECT_ID))
            .put("title", SearchQueryField.create(EventDefinitionDto.FIELD_TITLE))
            .put("description", SearchQueryField.create(EventDefinitionDto.FIELD_DESCRIPTION))
            .build();

    private final DBEventDefinitionService dbService;
    private final EventDefinitionHandler eventDefinitionHandler;
    private final EventDefinitionContextService contextService;
    private final EventProcessorEngine engine;
    private final SearchQueryParser searchQueryParser;
    private final RecentActivityService recentActivityService;
    private final BulkExecutor<EventDefinitionDto, UserContext> bulkExecutor;

    @Inject
    public EventDefinitionsResource(DBEventDefinitionService dbService,
                                    EventDefinitionHandler eventDefinitionHandler,
                                    EventDefinitionContextService contextService,
                                    EventProcessorEngine engine,
                                    RecentActivityService recentActivityService,
                                    AuditEventSender auditEventSender,
                                    ObjectMapper objectMapper
    ) {
        this.dbService = dbService;
        this.eventDefinitionHandler = eventDefinitionHandler;
        this.contextService = contextService;
        this.engine = engine;
        this.searchQueryParser = new SearchQueryParser(EventDefinitionDto.FIELD_TITLE, SEARCH_FIELD_MAPPING);
        this.recentActivityService = recentActivityService;
        this.bulkExecutor = new SequentialBulkExecutor<>(this::delete, auditEventSender, objectMapper);
    }

    @GET
    @ApiOperation("List event definitions")
    public PaginatedResponse<EventDefinitionDto> list(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                      @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                      @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query) {
        SearchQuery searchQuery;
        try {
            searchQuery = searchQueryParser.parse(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage());
        }
        final PaginatedList<EventDefinitionDto> result = dbService.searchPaginated(searchQuery, event -> {
            return isPermitted(RestPermissions.EVENT_DEFINITIONS_READ, event.id());
        }, "title", page, perPage);
        final ImmutableMap<String, Object> context = contextService.contextFor(result.delegate());
        return PaginatedResponse.create("event_definitions", result, query, context);
    }

    @GET
    @Path("{definitionId}")
    @ApiOperation("Get an event definition")
    public EventDefinitionDto get(@ApiParam(name = "definitionId") @PathParam("definitionId") @NotBlank String definitionId) {
        checkPermission(RestPermissions.EVENT_DEFINITIONS_READ, definitionId);
        return dbService.get(definitionId)
                .orElseThrow(() -> new NotFoundException("Event definition <" + definitionId + "> doesn't exist"));
    }

    @GET
    @Path("{definitionId}/with-context")
    @ApiOperation("Get an event definition")
    public Map<String, Object> getWithContext(@ApiParam(name = "definitionId") @PathParam("definitionId") @NotBlank String definitionId) {
        checkPermission(RestPermissions.EVENT_DEFINITIONS_READ, definitionId);
        return dbService.get(definitionId)
                .map(evenDefinition -> ImmutableMap.of(
                        "event_definition", evenDefinition,
                        "context", contextService.contextFor(evenDefinition)
                ))
                .orElseThrow(() -> new NotFoundException("Event definition <" + definitionId + "> doesn't exist"));
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Create new event definition")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_DEFINITION_CREATE)
    @RequiresPermissions(RestPermissions.EVENT_DEFINITIONS_CREATE)
    public Response create(@ApiParam("schedule") @QueryParam("schedule") @DefaultValue("true") boolean schedule,
                           @ApiParam(name = "JSON Body") EventDefinitionDto dto, @Context UserContext userContext) {
        checkEventDefinitionPermissions(dto, "create");

        final ValidationResult result = dto.validate();
        if (result.failed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
        final EventDefinitionDto entity = schedule ? eventDefinitionHandler.create(dto, Optional.of(userContext.getUser())) : eventDefinitionHandler.createWithoutSchedule(dto, Optional.of(userContext.getUser()));
        recentActivityService.create(entity.id(), GRNTypes.EVENT_DEFINITION, userContext.getUser());
        return Response.ok().entity(entity).build();
    }

    @PUT
    @Path("{definitionId}")
    @ApiOperation("Update existing event definition")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_DEFINITION_UPDATE)
    public Response update(@ApiParam(name = "definitionId") @PathParam("definitionId") @NotBlank String definitionId,
                           @ApiParam("schedule") @QueryParam("schedule") @DefaultValue("true") boolean schedule,
                           @ApiParam(name = "JSON Body") EventDefinitionDto dto,
                           @Context UserContext userContext) {
        checkPermission(RestPermissions.EVENT_DEFINITIONS_EDIT, definitionId);
        checkEventDefinitionPermissions(dto, "update");
        EventDefinitionDto oldDto = dbService.get(definitionId)
                .orElseThrow(() -> new NotFoundException("Event definition <" + definitionId + "> doesn't exist"));
        checkProcessorConfig(oldDto, dto);

        final ValidationResult result = dto.validate();
        if (!definitionId.equals(dto.id())) {
            result.addError("id", "Event definition IDs don't match");
        }

        if (result.failed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
        recentActivityService.update(definitionId, GRNTypes.EVENT_DEFINITION, userContext.getUser());
        return Response.ok().entity(eventDefinitionHandler.update(dto, schedule)).build();
    }

    @DELETE
    @Path("{definitionId}")
    @ApiOperation("Delete event definition")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_DEFINITION_DELETE)
    public EventDefinitionDto delete(@ApiParam(name = "definitionId") @PathParam("definitionId") @NotBlank String definitionId,
                                     @Context UserContext userContext) {
        checkPermission(RestPermissions.EVENT_DEFINITIONS_DELETE, definitionId);
        final Optional<EventDefinitionDto> eventDefinitionDto = dbService.get(definitionId);
        eventDefinitionDto.ifPresent(d ->
                recentActivityService.delete(d.id(), GRNTypes.EVENT_DEFINITION, d.title(), userContext.getUser())
        );
        eventDefinitionHandler.delete(definitionId);
        return eventDefinitionDto.orElse(null);
    }

    @POST
    @Path("/bulk_delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    @ApiOperation(value = "Delete a bulk of event definitions", response = BulkOperationResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Could not delete at least one of the event definitions in the bulk.")
    })
    @NoAuditEvent("Audit events triggered manually")
    public Response bulkDelete(@ApiParam(name = "Entities to remove", required = true) final BulkOperationRequest bulkOperationRequest,
                               @Context UserContext userContext) {

        final BulkOperationResponse response = bulkExecutor.executeBulkOperation(bulkOperationRequest,
                userContext,
                new AuditParams(EventsAuditEventTypes.EVENT_DEFINITION_DELETE, "definitionId", EventDefinitionDto.class));

        return Response.status(response.failures().isEmpty() ? Response.Status.OK : Response.Status.BAD_REQUEST)
                .entity(response)
                .build();
    }

    @PUT
    @Path("{definitionId}/schedule")
    @Consumes(MediaType.WILDCARD)
    @ApiOperation("Enable event definition")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_DEFINITION_UPDATE)
    public void schedule(@ApiParam(name = "definitionId") @PathParam("definitionId") @NotBlank String definitionId) {
        checkPermission(RestPermissions.EVENT_DEFINITIONS_EDIT, definitionId);
        eventDefinitionHandler.schedule(definitionId);
    }

    @PUT
    @Path("{definitionId}/unschedule")
    @Consumes(MediaType.WILDCARD)
    @ApiOperation("Disable event definition")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_DEFINITION_UPDATE)
    public void unschedule(@ApiParam(name = "definitionId") @PathParam("definitionId") @NotBlank String definitionId) {
        checkPermission(RestPermissions.EVENT_DEFINITIONS_EDIT, definitionId);
        eventDefinitionHandler.unschedule(definitionId);
    }

    @PUT
    @Path("{definitionId}/clear-notification-queue")
    @Consumes(MediaType.WILDCARD)
    @ApiOperation("Clear queued notifications for event")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_DEFINITION_CLEAR_NOTIFICATION_QUEUE)
    public void clearNotificationQueue(@ApiParam(name = "definitionId") @PathParam("definitionId") @NotBlank String definitionId) {
        checkPermission(RestPermissions.EVENT_DEFINITIONS_EDIT, definitionId);
        eventDefinitionHandler.deleteNotificationJobTriggers(definitionId);
    }

    @POST
    @ApiOperation("Execute event definition")
    @Path("{definitionId}/execute")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_DEFINITION_EXECUTE)
    public void execute(@ApiParam(name = "definitionId") @PathParam("definitionId") @NotBlank String definitionId,
                        @ApiParam(name = "parameters", required = true) @NotNull EventProcessorParameters parameters) {
        checkPermission(RestPermissions.EVENT_DEFINITIONS_EXECUTE, definitionId);
        if (parameters instanceof EventProcessorParametersWithTimerange.FallbackParameters) {
            throw new BadRequestException("Unknown parameters type");
        }

        try {
            engine.execute(definitionId, parameters);
        } catch (EventProcessorException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("/validate")
    @NoAuditEvent("Validation only")
    @ApiOperation(value = "Validate an event definition")
    @RequiresPermissions(RestPermissions.EVENT_DEFINITIONS_CREATE)
    public ValidationResult validate(@ApiParam(name = "JSON body", required = true)
                                     @Valid @NotNull EventDefinitionDto toValidate) {
        return toValidate.config().validate();
    }

    private void checkEventDefinitionPermissions(EventDefinitionDto dto, String action) {
        final Set<String> missingPermissions = dto.requiredPermissions().stream()
                .filter(permission -> !isPermitted(permission))
                .collect(Collectors.toSet());

        if (!missingPermissions.isEmpty()) {
            LOG.info("Not authorized to {} event definition. User <{}> is missing permissions: {}", action, getSubject().getPrincipal(), missingPermissions);
            throw new ForbiddenException("Not authorized");
        }
    }

    /**
     * Check that if this Event Definitions Processor Config is being modified, it is allowed to be.
     * @param oldEventDefinition - The Existing Event Definition
     * @param updatedEventDefinition - The Event Definition with pending updates
     */
    @VisibleForTesting
    void checkProcessorConfig(EventDefinitionDto oldEventDefinition, EventDefinitionDto updatedEventDefinition) {
        if (!oldEventDefinition.config().isUserPresentable()
                && !oldEventDefinition.config().type().equals(updatedEventDefinition.config().type())) {
            LOG.error("Not allowed to change event definition condition type from <{}> to <{}>.",
                    oldEventDefinition.config().type(), updatedEventDefinition.config().type());
            throw new ForbiddenException("Condition type not changeable");
        }
    }
}
