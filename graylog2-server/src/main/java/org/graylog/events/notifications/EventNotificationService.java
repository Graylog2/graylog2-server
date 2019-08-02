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
package org.graylog.events.notifications;

import com.google.common.collect.ImmutableList;
import org.graylog.events.configuration.EventsConfigurationProvider;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.MessageSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class EventNotificationService {
    private static final Logger LOG = LoggerFactory.getLogger(EventNotificationService.class);

    private final EventBacklogService eventBacklogService;
    private final EventsConfigurationProvider configurationProvider;

    @Inject
    public EventNotificationService(EventBacklogService eventBacklogService,
                                    EventsConfigurationProvider configurationProvider) {
        this.eventBacklogService = eventBacklogService;
        this.configurationProvider = configurationProvider;
    }

    public ImmutableList<MessageSummary> getBacklogForEvent(EventNotificationContext ctx) {
        final ImmutableList<MessageSummary> backlog;
        try {
            if (ctx.eventDefinition().isPresent()) {
                final long backlogSize = ctx.eventDefinition().get().notificationSettings().backlogSize();
                if (backlogSize <= 0) {
                    return ImmutableList.of();
                }
                backlog = eventBacklogService.getMessagesForEvent(ctx.event(), backlogSize);
            } else {
                backlog = eventBacklogService.getMessagesForEvent(ctx.event(), configurationProvider.get().eventNotificationsBacklog());
            }
        } catch (NotFoundException e) {
            LOG.error("Failed to fetch backlog for event {}", ctx.event().id());
            return ImmutableList.of();
        }
        return backlog;
    }
}
