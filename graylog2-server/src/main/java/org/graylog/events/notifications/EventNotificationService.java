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
