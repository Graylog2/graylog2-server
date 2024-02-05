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
package org.graylog2.notifications;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import org.graylog2.streams.events.StreamDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeletedStreamNotificationListener {

    private static final Logger LOG = LoggerFactory.getLogger(DeletedStreamNotificationListener.class);

    private final NotificationService notificationService;

    @Inject
    public DeletedStreamNotificationListener(EventBus eventBus, NotificationService notificationService) {
        this.notificationService = notificationService;
        eventBus.register(this);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleStreamDeleted(StreamDeletedEvent streamDeletedEvent) {
        final String streamId = streamDeletedEvent.streamId();
        for (Notification notification : notificationService.all()) { // TODO: this could be optimized to select only notifications linked to the stream ID
            Object rawValue = notification.getDetail("stream_id");
            if (rawValue != null && rawValue.toString().equals(streamId)) {
                LOG.debug("Removing notification that references stream: {}", notification);
                notificationService.destroy(notification);
            }
        }
    }
}
