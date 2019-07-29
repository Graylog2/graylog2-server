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
import org.graylog.events.audit.EventsAuditEventTypes;
import org.graylog.events.notifications.DBNotificationService;
import org.graylog.events.notifications.NotificationDto;
import org.graylog.events.notifications.NotificationResourceHandler;
import org.graylog2.alarmcallbacks.EmailAlarmCallback;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;

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
            .put("id", SearchQueryField.create(NotificationDto.FIELD_ID))
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
        return PaginatedResponse.create("notifications", dbNotificationService.getAllPaginated(searchQuery, "title", page, perPage), query);
    }

    @GET
    @Path("/{notificationId}")
    @ApiOperation("Get a notification")
    public NotificationDto get(@ApiParam(name = "notificationId") @PathParam("notificationId") @NotBlank String notificationId) {
        return dbNotificationService.get(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification " + notificationId + " doesn't exist"));
    }

    @POST
    @ApiOperation("Create new notification definition")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_NOTIFICATION_CREATE)
    public NotificationDto create(NotificationDto dto) {
        return resourceHandler.create(dto);
    }

    @PUT
    @Path("/{notificationId}")
    @ApiOperation("Update existing notification")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_NOTIFICATION_UPDATE)
    public NotificationDto update(@ApiParam(name = "notificationId") @PathParam("notificationId") @NotBlank String notificationId,
                                  NotificationDto dto) {
        dbNotificationService.get(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification " + notificationId + " doesn't exist"));

        if (!notificationId.equals(dto.id())) {
            throw new BadRequestException("Notification IDs don't match");
        }

        return resourceHandler.update(dto);
    }

    @DELETE
    @Path("/{notificationId}")
    @ApiOperation("Delete a notification")
    @AuditEvent(type = EventsAuditEventTypes.EVENT_NOTIFICATION_DELETE)
    public void delete(@ApiParam(name = "notificationId") @PathParam("notificationId") @NotBlank String notificationId) {
        resourceHandler.delete(notificationId);
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
