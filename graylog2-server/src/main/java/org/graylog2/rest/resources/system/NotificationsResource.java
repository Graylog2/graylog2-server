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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.auditlog.jersey.AuditLog;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RequiresAuthentication
@Api(value = "System/Notifications", description = "Notifications generated by the system")
@Path("/system/notifications")
public class NotificationsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationsResource.class);

    private final NotificationService notificationService;

    @Inject
    public NotificationsResource(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get all active notifications")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> listNotifications() {
        final List<Map<String, Object>> notifications = Lists.newArrayList();
        for (Notification n : notificationService.all()) {
            final String notificationType = n.getType().toString().toLowerCase(Locale.ENGLISH);
            if (!isPermitted(RestPermissions.NOTIFICATIONS_READ, notificationType)) {
                continue;
            }

            final Map<String, Object> notification = n.asMap();
            try {
                notifications.add(notification);
            } catch (IllegalArgumentException e) {
                LOG.warn("There is a notification type we can't handle: [" + n.getType() + "]");
            }
        }

        return ImmutableMap.of(
                "total", notifications.size(),
                "notifications", notifications);
    }

    @DELETE
    @Timed
    @Path("/{notificationType}")
    @ApiOperation(value = "Delete a notification")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such notification type.")
    })
    @AuditLog(object = "system notification")
    public void deleteNotification(@ApiParam(name = "notificationType")
                                   @PathParam("notificationType") String notificationType) {
        Notification.Type type;
        checkPermission(RestPermissions.NOTIFICATIONS_DELETE, notificationType);
        try {
            type = Notification.Type.valueOf(notificationType.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            LOG.warn("No such notification type: [" + notificationType + "]");
            throw new BadRequestException(e);
        }

        notificationService.destroyAllByType(type);
    }
}
