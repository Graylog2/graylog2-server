package org.graylog.events.notifications;

import com.google.common.collect.ImmutableList;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.MessageSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class EventNotificationService {
    private static final Logger LOG = LoggerFactory.getLogger(EventNotificationService.class);

    private final EventBacklogService eventBacklogService;

    @Inject
    public EventNotificationService(EventBacklogService eventBacklogService) {
        this.eventBacklogService = eventBacklogService;
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
                backlog = eventBacklogService.getMessagesForEvent(ctx.event(), 50);
            }
        } catch (NotFoundException e) {
            LOG.error("Failed to fetch backlog for event {}", ctx.event().id());
            return ImmutableList.of();
        }
        return backlog;
    }
}
