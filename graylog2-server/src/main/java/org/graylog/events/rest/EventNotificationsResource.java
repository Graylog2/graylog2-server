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
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.events.audit.EventsAuditEventTypes;
import org.graylog.events.notifications.DBNotificationService;
import org.graylog.events.notifications.NotificationDto;
import org.graylog.events.notifications.NotificationResourceHandler;
import org.graylog.security.UserContext;
import org.graylog2.alarmcallbacks.EmailAlarmCallback;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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
import java.util.Set;

import static org.graylog2.shared.security.RestPermissions.USERS_LIST;

@Api(value = "Events/Notifications", description = "Manage event notifications")
@Path("/events/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class EventNotificationsResource extends RestResource implements PluginRestResource {
    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create("_id", SearchQueryField.Type.ID))
            .put("title", SearchQueryField.create(NotificationDto.FIELD_TITLE))
            .put("description", SearchQueryField.create(NotificationDto.FIELD_DESCRIPTION))
            .build();

    private final DBNotificationService dbNotificationService;
    private final Set<AlarmCallback> availableLegacyAlarmCallbacks;
    private final SearchQueryParser searchQueryParser;
    private final NotificationResourceHandler resourceHandler;

    @Inject
    public EventNotificationsResource(DBNotificationService dbNotificationService,
                                      Set<AlarmCallback> availableLegacyAlarmCallbacks,
                                      NotificationResourceHandler resourceHandler) {
        this.dbNotificationService = dbNotificationService;
        this.availableLegacyAlarmCallbacks = availableLegacyAlarmCallbacks;
        this.resourceHandler = resourceHandler;
        this.searchQueryParser = new SearchQueryParser(NotificationDto.FIELD_TITLE, SEARCH_FIELD_MAPPING);
    }

    @GET
    @ApiOperation("List all available notifications")
    public PaginatedResponse<NotificationDto> listNotifications(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                                @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                                @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query) {
        final SearchQuery searchQuery = searchQueryParser.parse(query);
        final PaginatedList<NotificationDto> result = dbNotificationService.searchPaginated(searchQuery, notification -> {
            return isPermitted(RestPermissions.EVENT_NOTIFICATIONS_READ, notification.id());
        }, "title", page, perPage);
        return PaginatedResponse.create("notifications", result, query);
    }

    @GET
    @Path("/{notificationId}")
    @ApiOperation("Get a notification")
    public NotificationDto get(@ApiParam(name = "notificationId") @PathParam("notificationId") @NotBlank String notificationId) {
        checkPermission(RestPermissions.EVENT_NOTIFICATIONS_READ, notificationId);
        return dbNotificationService.get(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification " + notificationId + " doesn't exist"));
    }

    @POST
    @ApiOperation("Create new notification definition")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_NOTIFICATION_CREATE)
    @RequiresPermissions(RestPermissions.EVENT_NOTIFICATIONS_CREATE)
    public Response create(@ApiParam(name = "JSON Body") NotificationDto dto, @Context UserContext userContext) {
        final ValidationResult validationResult = dto.validate();
        if (validationResult.failed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(validationResult).build();
        }
        return Response.ok().entity(resourceHandler.create(dto, java.util.Optional.ofNullable(userContext.getUser()))).build();
    }

    @PUT
    @Path("/{notificationId}")
    @ApiOperation("Update existing notification")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_NOTIFICATION_UPDATE)
    public Response update(@ApiParam(name = "notificationId") @PathParam("notificationId") @NotBlank String notificationId,
                                  @ApiParam(name = "JSON Body") NotificationDto dto) {
        checkPermission(RestPermissions.EVENT_NOTIFICATIONS_EDIT, notificationId);
        dbNotificationService.get(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification " + notificationId + " doesn't exist"));

        if (!notificationId.equals(dto.id())) {
            throw new BadRequestException("Notification IDs don't match");
        }

        final ValidationResult validationResult = dto.validate();
        if (validationResult.failed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(validationResult).build();
        }

        return Response.ok().entity(resourceHandler.update(dto)).build();
    }

    @DELETE
    @Path("/{notificationId}")
    @ApiOperation("Delete a notification")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_NOTIFICATION_DELETE)
    public void delete(@ApiParam(name = "notificationId") @PathParam("notificationId") @NotBlank String notificationId) {
        checkPermission(RestPermissions.EVENT_NOTIFICATIONS_DELETE, notificationId);
        resourceHandler.delete(notificationId);
    }

    @POST
    @Timed
    @Path("/{notificationId}/test")
    @ApiOperation(value = "Send a test alert for a given event notification")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Event notification not found."),
            @ApiResponse(code = 500, message = "Error while testing event notification")
    })
    @NoAuditEvent("only used to test event notifications")
    public Response test(@ApiParam(name = "notificationId", value = "The event notification id to send a test alert for.", required = true)
                         @PathParam("notificationId") @NotBlank String notificationId) {
        checkPermission(RestPermissions.EVENT_NOTIFICATIONS_EDIT, notificationId);
        final NotificationDto notificationDto =
                dbNotificationService.get(notificationId).orElseThrow(() -> new NotFoundException("Notification " + notificationId + " doesn't exist"));

        resourceHandler.test(notificationDto, getSubject().getPrincipal().toString());

        return Response.ok().build();
    }

    @POST
    @Timed
    @Path("/test")
    @RequiresPermissions(RestPermissions.EVENT_NOTIFICATIONS_CREATE)
    @ApiOperation(value = "Send a test alert for a given event notification")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Event notification is invalid."),
            @ApiResponse(code = 500, message = "Error while testing event notification")
    })
    @NoAuditEvent("only used to test event notifications")
    public Response test(@ApiParam(name = "JSON Body") NotificationDto dto) {
        checkPermission(RestPermissions.EVENT_NOTIFICATIONS_CREATE);
        final ValidationResult validationResult = dto.validate();
        if (validationResult.failed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(validationResult).build();
        }

        resourceHandler.test(dto, getSubject().getPrincipal().toString());

        return Response.ok().build();
    }

    @GET
    @Path("/legacy/types")
    @ApiOperation("List all available legacy alarm callback types")
    public Response legacyTypes() {
        final ImmutableMap.Builder<String, Map<String, Object>> typesBuilder = ImmutableMap.builder();

        for (AlarmCallback availableAlarmCallback : availableLegacyAlarmCallbacks) {
            typesBuilder.put(availableAlarmCallback.getClass().getCanonicalName(), ImmutableMap.of(
                    "name", availableAlarmCallback.getName(),
                    "configuration", getConfigurationRequest(availableAlarmCallback).asList()
            ));
        }

        return Response.ok(ImmutableMap.of("types", typesBuilder.build())).build();
    }

    // This is used to add user auto-completion to EmailAlarmCallback when the current user has permissions to list users
    private ConfigurationRequest getConfigurationRequest(AlarmCallback callback) {
        if (callback instanceof EmailAlarmCallback && isPermitted(USERS_LIST)) {
            return ((EmailAlarmCallback) callback).getEnrichedRequestedConfiguration();
        }
        return callback.getRequestedConfiguration();
    }
}
