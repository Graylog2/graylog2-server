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
package org.graylog2.users;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.dashboards.events.DashboardDeletedEvent;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.events.StreamDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class UserPermissionsCleanupListener {
    private static final Logger LOG = LoggerFactory.getLogger(UserPermissionsCleanupListener.class);

    private final UserService userService;

    @Inject
    public UserPermissionsCleanupListener(EventBus serverEventBus,
                                          UserService userService) {
        this.userService = userService;
        serverEventBus.register(this);
    }

    @Subscribe
    public void cleanupPermissionsOnDashboardRemoval(DashboardDeletedEvent event) {
        final List<String> dashboardPermissions = Arrays.asList(
                RestPermissions.DASHBOARDS_READ + ":" + event.dashboardId(),
                RestPermissions.DASHBOARDS_EDIT + ":" + event.dashboardId());
        removePermissionsFromAllUsers(dashboardPermissions);
    }

    @Subscribe
    public void cleanupPermissionsOnStreamRemoval(StreamDeletedEvent event) {
        final List<String> streamPermissions = Arrays.asList(
                RestPermissions.STREAMS_READ + ":" + event.streamId(),
                RestPermissions.STREAMS_EDIT + ":" + event.streamId(),
                RestPermissions.STREAMS_CHANGESTATE + ":" + event.streamId()
        );
        removePermissionsFromAllUsers(streamPermissions);
    }

    private void removePermissionsFromAllUsers(Collection<String> permissions) {
        final List<User> users = userService.loadAll();
        for (User user : users) {
            final List<String> userPermissions = new ArrayList<>(user.getPermissions());
            boolean modifiedUser = userPermissions.removeAll(permissions);

            if (modifiedUser) {
                user.setPermissions(userPermissions);
                try {
                    final String userId = userService.save(user);
                    LOG.debug("Successfully updated permissions of user <{}>: {}", userId, userPermissions);
                } catch (ValidationException e) {
                    LOG.warn("Unable to save user while removing permissions of deleted dashboard: ", e);
                }
            }
        }
    }
}
