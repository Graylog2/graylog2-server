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

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.dashboards.events.DashboardDeletedEvent;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        final Set<String> permissions = ImmutableSet.of(
                "dashboards:read:" + event.dashboardId(),
                "dashboards:edit:" + event.dashboardId()
        );
        this.userService.loadAll().forEach(user -> {
            List<String> userPermissions = new ArrayList<>(user.getPermissions());
            boolean modifiedUser = false;
            for (String permission : permissions) {
                if (userPermissions.contains(permission)) {
                    userPermissions.remove(permission);
                    modifiedUser = true;
                }
            }

            if (modifiedUser) {
                user.setPermissions(userPermissions);
                try {
                    userService.save(user);
                } catch (ValidationException e) {
                    LOG.warn("Unable to save user while removing permissions of deleted dashboard: ", e);
                }
            }
        });
    }
}
