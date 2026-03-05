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
package org.graylog2.rest.resources.system;

import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationsResourceTest {

    @Mock
    private NotificationService notificationService;

    @Test
    void cloudModeSuppressesSuppressedTypesAndKeepsOthers() {
        assertThat(Notification.CLOUD_SUPPRESSED_TYPES).contains(Notification.Type.ES_CLUSTER_RED);

        when(notificationService.all()).thenReturn(List.of(
                new NotificationImpl().addType(Notification.Type.ES_CLUSTER_RED),
                new NotificationImpl().addType(Notification.Type.SEARCH_ERROR)
        ));

        final var response = new TestResource(notificationService, true).listNotifications();

        assertThat(response.notifications())
                .extracting(Notification::getType)
                .containsExactly(Notification.Type.SEARCH_ERROR);
    }

    @Test
    void nonCloudModeReturnsAll() {
        when(notificationService.all()).thenReturn(List.of(
                new NotificationImpl().addType(Notification.Type.ES_CLUSTER_RED),
                new NotificationImpl().addType(Notification.Type.SEARCH_ERROR)
        ));

        final var response = new TestResource(notificationService, false).listNotifications();

        assertThat(response.notifications()).hasSize(2);
    }

    static class TestResource extends NotificationsResource {
        TestResource(NotificationService notificationService, boolean isCloud) {
            super(notificationService, isCloud);
        }

        @Override
        protected boolean isPermitted(String permission, String instanceId) {
            return true;
        }
    }
}
