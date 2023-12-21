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
import org.graylog.events.notifications.types.EmailEventNotificationConfig;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityService;
import org.graylog.security.UserContext;
import org.graylog2.alarmcallbacks.EmailAlarmCallback;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.configuration.EmailConfiguration;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import jakarta.inject.Inject;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import jakarta.validation.constraints.NotBlank;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;
import static org.graylog2.shared.security.RestPermissions.USERS_LIST;

@Api(value = "Events/Notifications", description = "Manage event notifications", tags = {CLOUD_VISIBLE})
@Path("/events/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class EventNotificationsResource extends RestResource implements PluginRestResource {
    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create("_id", SearchQueryField.Type.OBJECT_ID))
            .put("title", SearchQueryField.create(NotificationDto.FIELD_TITLE))
            .put("description", SearchQueryField.create(NotificationDto.FIELD_DESCRIPTION))
            .build();

    private static final String DEFAULT_SORT_FIELD = "title";
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    private static final List<EntityAttribute> attributes = List.of(
            EntityAttribute.builder().id("title").title("Title").build(),
            EntityAttribute.builder().id("description").title("Description").build(),
            EntityAttribute.builder().id("type").title("Type").build()
    );
    private static final EntityDefaults settings = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT))))
            .build();

    private final DBNotificationService dbNotificationService;
    private final Set<AlarmCallback> availableLegacyAlarmCallbacks;
    private final SearchQueryParser searchQueryParser;
    private final NotificationResourceHandler resourceHandler;
    private final EmailConfiguration emailConfiguration;
    private final RecentActivityService recentActivityService;

    @Inject
    public EventNotificationsResource(DBNotificationService dbNotificationService,
                                      Set<AlarmCallback> availableLegacyAlarmCallbacks,
                                      NotificationResourceHandler resourceHandler,
                                      EmailConfiguration emailConfiguration,
                                      RecentActivityService recentActivityService) {
        this.dbNotificationService = dbNotificationService;
        this.availableLegacyAlarmCallbacks = availableLegacyAlarmCallbacks;
        this.resourceHandler = resourceHandler;
        this.searchQueryParser = new SearchQueryParser(NotificationDto.FIELD_TITLE, SEARCH_FIELD_MAPPING);
        this.emailConfiguration = emailConfiguration;
        this.recentActivityService = recentActivityService;

    }

    @GET
    @Timed
    @Path("/paginated")
    @ApiOperation(value = "Get a paginated list of event notifications")
    @Produces(MediaType.APPLICATION_JSON)
    public PageListResponse<NotificationDto> getPage(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                     @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                     @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                     @ApiParam(name = "sort",
                                                               value = "The field to sort the result on",
                                                               required = true,
                                                               allowableValues = "title,description,type")
                                                     @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
                                                     @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                                     @DefaultValue(DEFAULT_SORT_DIRECTION) @QueryParam("order") String order) {
        final SearchQuery searchQuery = searchQueryParser.parse(query);
        if ("type".equals(sort)) {
            sort = "config.type";
        }
        final PaginatedList<NotificationDto> result = dbNotificationService.searchPaginated(searchQuery, notification -> {
            return isPermitted(RestPermissions.EVENT_NOTIFICATIONS_READ, notification.id());
        }, sort, order, page, perPage);


        return PageListResponse.create(query, result.pagination(),
                result.grandTotal().orElse(0L), sort, order, result.delegate(), attributes, settings);
    }

    @GET
    @ApiOperation("List all available notifications")
    @Deprecated
    public PaginatedResponse<NotificationDto> listNotifications(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                                @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                                @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query) {
        final SearchQuery searchQuery = searchQueryParser.parse(query);
        final PaginatedList<NotificationDto> result = dbNotificationService.searchPaginated(searchQuery, notification -> {
            return isPermitted(RestPermissions.EVENT_NOTIFICATIONS_READ, notification.id());
        }, "title", "asc", page, perPage);
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
        validateEmailConfiguration(dto, validationResult);
        if (validationResult.failed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(validationResult).build();
        }
        var entity = resourceHandler.create(dto, java.util.Optional.ofNullable(userContext.getUser()));
        recentActivityService.create(entity.id(), GRNTypes.EVENT_NOTIFICATION, userContext.getUser());
        return Response.ok().entity(entity).build();
    }

    @PUT
    @Path("/{notificationId}")
    @ApiOperation("Update existing notification")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_NOTIFICATION_UPDATE)
    public Response update(@ApiParam(name = "notificationId") @PathParam("notificationId") @NotBlank String notificationId,
                           @ApiParam(name = "JSON Body") NotificationDto dto,
                           @Context UserContext userContext) {
        checkPermission(RestPermissions.EVENT_NOTIFICATIONS_EDIT, notificationId);
        if (dbNotificationService.get(notificationId).isEmpty()) {
            throw new NotFoundException("Notification " + notificationId + " doesn't exist");
        }

        if (!notificationId.equals(dto.id())) {
            throw new BadRequestException("Notification IDs don't match");
        }

        final ValidationResult validationResult = dto.validate();
        validateEmailConfiguration(dto, validationResult);
        if (validationResult.failed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(validationResult).build();
        }

        recentActivityService.update(notificationId, GRNTypes.EVENT_NOTIFICATION, userContext.getUser());
        return Response.ok().entity(resourceHandler.update(dto)).build();
    }

    private void validateEmailConfiguration(NotificationDto dto, ValidationResult validationResult) {
        if (dto.config() instanceof EmailEventNotificationConfig) {
            EmailEventNotificationConfig emailEventNotificationConfig = (EmailEventNotificationConfig) dto.config();
            if (!emailConfiguration.isEnabled()) {
                validationResult.addError("config", "Email transport is not configured in graylog.conf");
            }
            if (isNullOrEmpty(emailConfiguration.getFromEmail()) && isNullOrEmpty(emailEventNotificationConfig.sender())) {
                validationResult.addError("sender", "No default sender specified in graylog.conf. You must specify one here.");
            } else {
                if (!isNullOrEmpty(emailEventNotificationConfig.sender())) {
                    try {
                        InternetAddress email = new InternetAddress(emailEventNotificationConfig.sender());
                        email.validate();
                    } catch (AddressException e) {
                        validationResult.addError("sender", "Invalid email address.");
                    }
                }
                if (!isNullOrEmpty(emailConfiguration.getFromEmail())) {
                    try {
                        InternetAddress email = new InternetAddress(emailConfiguration.getFromEmail());
                        email.validate();
                    } catch (AddressException e) {
                        validationResult.addError("sender", "Invalid default sender email address specified in graylog.conf.");
                    }
                }
            }
        }
    }

    @DELETE
    @Path("/{notificationId}")
    @ApiOperation("Delete a notification")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_NOTIFICATION_DELETE)
    public void delete(@ApiParam(name = "notificationId") @PathParam("notificationId") @NotBlank String notificationId,
                       @Context UserContext userContext) {
        checkPermission(RestPermissions.EVENT_NOTIFICATIONS_DELETE, notificationId);
        dbNotificationService.get(notificationId).ifPresent(n ->
                recentActivityService.delete(notificationId, GRNTypes.EVENT_NOTIFICATION, n.title(), userContext.getUser())
        );
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
