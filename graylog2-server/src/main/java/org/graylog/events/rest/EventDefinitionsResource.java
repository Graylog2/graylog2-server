/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.events.rest;


import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.events.audit.EventsAuditEventTypes;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventDefinitionHandler;
import org.graylog.events.processor.EventProcessorEngine;
import org.graylog.events.processor.EventProcessorException;
import org.graylog.events.processor.EventProcessorParameters;
import org.graylog.events.processor.EventProcessorParametersWithTimerange;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.plugin.rest.ValidationResult;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.util.stream.Collectors;

@Api(value = "Events/Definitions", description = "Event definition management")
@Path("/events/definitions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class EventDefinitionsResource extends RestResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(EventDefinitionsResource.class);

    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create(EventDefinitionDto.FIELD_ID))
            .put("title", SearchQueryField.create(EventDefinitionDto.FIELD_TITLE))
            .put("description", SearchQueryField.create(EventDefinitionDto.FIELD_DESCRIPTION))
            .build();

    private final DBEventDefinitionService dbService;
    private final EventDefinitionHandler eventDefinitionHandler;
    private final EventProcessorEngine engine;
    private final SearchQueryParser searchQueryParser;

    @Inject
    public EventDefinitionsResource(DBEventDefinitionService dbService,
                                    EventDefinitionHandler eventDefinitionHandler,
                                    EventProcessorEngine engine) {
        this.dbService = dbService;
        this.eventDefinitionHandler = eventDefinitionHandler;
        this.engine = engine;
        this.searchQueryParser = new SearchQueryParser(EventDefinitionDto.FIELD_TITLE, SEARCH_FIELD_MAPPING);
    }

    @GET
    @ApiOperation("List event definitions")
    public PaginatedResponse<EventDefinitionDto> list(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                      @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                      @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query) {
        final SearchQuery searchQuery = searchQueryParser.parse(query);
        final PaginatedList<EventDefinitionDto> result = dbService.searchPaginated(searchQuery, event -> {
            return isPermitted(RestPermissions.EVENT_DEFINITIONS_READ, event.id());
            }, "title", page, perPage);
        return PaginatedResponse.create("event_definitions", result, query);
    }

    @GET
    @Path("{definitionId}")
    @ApiOperation("Get an event definition")
    public EventDefinitionDto get(@ApiParam(name = "definitionId") @PathParam("definitionId") @NotBlank String definitionId) {
        checkPermission(RestPermissions.EVENT_DEFINITIONS_READ, definitionId);
        return dbService.get(definitionId)
                .orElseThrow(() -> new NotFoundException("Event definition <" + definitionId + "> doesn't exist"));
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Create new event definition")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_DEFINITION_CREATE)
    @RequiresPermissions(RestPermissions.EVENT_DEFINITIONS_CREATE)
    public Response create(@ApiParam(name = "JSON Body") EventDefinitionDto dto) {
        checkEventDefinitionPermissions(dto, "create");

        final ValidationResult result = dto.validate();
        if (result.failed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
        return Response.ok().entity(eventDefinitionHandler.create(dto)).build();
    }

    @PUT
    @Path("{definitionId}")
    @ApiOperation("Update existing event definition")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_DEFINITION_UPDATE)
    public Response update(@ApiParam(name = "definitionId") @PathParam("definitionId") @NotBlank String definitionId,
                           @ApiParam(name = "JSON Body") EventDefinitionDto dto) {
        checkPermission(RestPermissions.EVENT_DEFINITIONS_EDIT, definitionId);
        checkEventDefinitionPermissions(dto, "update");
        dbService.get(definitionId)
                .orElseThrow(() -> new NotFoundException("Event definition <" + definitionId + "> doesn't exist"));

        final ValidationResult result = dto.validate();
        if (!definitionId.equals(dto.id())) {
            result.addError("id", "Event definition IDs don't match");
        }

        if (result.failed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
        return Response.ok().entity(eventDefinitionHandler.update(dto)).build();
    }

    @DELETE
    @Path("{definitionId}")
    @ApiOperation("Delete event definition")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_DEFINITION_DELETE)
    public void delete(@ApiParam(name = "definitionId") @PathParam("definitionId") @NotBlank String definitionId) {
        checkPermission(RestPermissions.EVENT_DEFINITIONS_DELETE, definitionId);
        eventDefinitionHandler.delete(definitionId);
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
}
