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
package org.graylog.events.legacy;

import com.google.common.collect.ImmutableList;
import org.graylog.events.notifications.EventNotification;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.EventNotificationService;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog2.plugin.MessageSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Optional;

public class LegacyAlarmCallbackEventNotification implements EventNotification {
    public interface Factory extends EventNotification.Factory {
        @Override
        LegacyAlarmCallbackEventNotification create();
    }

    private static final Logger LOG = LoggerFactory.getLogger(LegacyAlarmCallbackEventNotification.class);

    private final EventNotificationService notificationCallbackService;
    private final LegacyAlarmCallbackSender alarmCallbackSender;

    @Inject
    public LegacyAlarmCallbackEventNotification(EventNotificationService notificationCallbackService,
                                                LegacyAlarmCallbackSender alarmCallbackSender) {
        this.notificationCallbackService = notificationCallbackService;
        this.alarmCallbackSender = alarmCallbackSender;
    }

    @Override
    public void execute(EventNotificationContext ctx) throws PermanentEventNotificationException {
        final LegacyAlarmCallbackEventNotificationConfig config = (LegacyAlarmCallbackEventNotificationConfig) ctx.notificationConfig();
        final ImmutableList<MessageSummary> messagesForEvent = notificationCallbackService.getBacklogForEvent(ctx);
        final Optional<EventDefinitionDto> optionalEventDefinition = ctx.eventDefinition();

        if (!optionalEventDefinition.isPresent()) {
            final String msg = String.format(Locale.ROOT, "Unable to find definition for event <%s>", ctx.event().id());
            LOG.error(msg);
            throw new PermanentEventNotificationException(msg);
        }

        try {
            alarmCallbackSender.send(config, optionalEventDefinition.get(), ctx.event(), messagesForEvent);
        } catch (Exception e) {
            // TODO: Is there a case where we want to retry? (and are able to detect when to do it)
            throw new PermanentEventNotificationException("Couldn't send legacy notification - legacy notifications cannot be retried!", e);
        }

    }
}
