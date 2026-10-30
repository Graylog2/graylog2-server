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
package org.graylog2.migrations;

import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class V20261029120000_RemoveShardAllocationMaximumNotificationsTest {

    @Test
    @SuppressWarnings("deprecation")
    void removesShardAllocationMaximumNotifications() {
        final NotificationService notificationService = mock(NotificationService.class);

        new V20261029120000_RemoveShardAllocationMaximumNotifications(notificationService).upgrade();

        verify(notificationService).fixed(Notification.Type.ES_SHARD_ALLOCATION_MAXIMUM);
        verifyNoMoreInteractions(notificationService);
    }
}
