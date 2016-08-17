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
import org.graylog2.rest.models.users.requests.Startpage;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.events.StreamDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class StartPageCleanupListener {
    private static final Logger LOG = LoggerFactory.getLogger(StartPageCleanupListener.class);

    private final UserService userService;

    @Inject
    public StartPageCleanupListener(EventBus serverEventBus,
                                    UserService userService) {
        this.userService = userService;
        serverEventBus.register(this);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void removeStartpageReferencesIfStreamDeleted(StreamDeletedEvent streamDeletedEvent) {
        final Startpage deletedStartpage = Startpage.create("stream", streamDeletedEvent.streamId());
        resetReferencesToStartpage(deletedStartpage);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void removeStartpageReferencesIfDashboardDeleted(DashboardDeletedEvent dashboardDeletedEvent) {
        final Startpage deletedStartpage = Startpage.create("dashboard", dashboardDeletedEvent.dashboardId());
        resetReferencesToStartpage(deletedStartpage);
    }

    private void resetReferencesToStartpage(Startpage deletedStartpage) {
        this.userService.loadAll()
            .stream()
            .filter(user -> user.getStartpage() != null && user.getStartpage().equals(deletedStartpage))
            .forEach(user -> {
                user.setStartpage(null);
                try {
                    this.userService.save(user);
                } catch (ValidationException e) {
                    LOG.error("Unable to reset start page for user which references deleted start page: ", e);
                }
            });
    }
}
