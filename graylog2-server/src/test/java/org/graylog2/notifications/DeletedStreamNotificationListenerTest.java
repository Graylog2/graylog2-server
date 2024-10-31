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
import org.assertj.core.api.Assertions;
import org.graylog2.streams.events.StreamDeletedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

class DeletedStreamNotificationListenerTest {


    @Test
    void testNotificationDeletion() {
        EventBus eventBus = new EventBus();
        final NotificationService notificationService = mockNotificationService("123", "456");
        new DeletedStreamNotificationListener(eventBus, notificationService);

        eventBus.post(StreamDeletedEvent.create("123"));

        final ArgumentCaptor<Notification> argumentCaptor = ArgumentCaptor.forClass(Notification.class);
        Mockito.verify(notificationService, Mockito.times(1)).destroy(argumentCaptor.capture());
        Assertions.assertThat(argumentCaptor.getValue().getDetail("stream_id"))
                .isEqualTo("123");
    }

    private NotificationService mockNotificationService(String... streamIDs) {
        final List<Notification> allNotifications = Arrays.stream(streamIDs).map(id -> new NotificationImpl().addDetail("stream_id", id)).toList();
        final NotificationService notificationService = Mockito.mock(NotificationService.class);
        Mockito.when(notificationService.all()).thenReturn(allNotifications);
        return notificationService;
    }
}
