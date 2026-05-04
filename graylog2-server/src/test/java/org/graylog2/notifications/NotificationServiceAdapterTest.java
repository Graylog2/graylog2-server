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

import org.graylog2.plugin.system.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceAdapterTest {

    @Mock
    private SystemNotificationService systemNotificationService;

    @Mock
    private NodeId nodeId;

    @Mock
    private NotificationSystemEventPublisher eventPublisher;

    private NotificationServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new NotificationServiceAdapter(systemNotificationService, nodeId, eventPublisher);
    }

    @Test
    void buildReturnsNotificationBuilder() {
        final Notification notification = adapter.build();
        assertThat(notification).isInstanceOf(NotificationBuilder.class);
    }

    @Test
    void buildNowSetsTimestamp() {
        final Notification notification = adapter.buildNow();
        assertThat(notification.getTimestamp()).isNotNull();
    }

    @Test
    void fixedByTypeCallsMarkAsReadWithSystemActor() {
        adapter.fixed(Notification.Type.ES_UNAVAILABLE);

        final ArgumentCaptor<SystemNotificationDto.Actor> actorCaptor =
                ArgumentCaptor.forClass(SystemNotificationDto.Actor.class);
        verify(systemNotificationService).markAsRead(eq(Notification.Type.ES_UNAVAILABLE), actorCaptor.capture());
        assertThat(actorCaptor.getValue().id()).isEqualTo("system");
    }

    @Test
    void fixedByTypeAndKeyCallsMarkAsReadWithKey() {
        adapter.fixed(Notification.Type.INPUT_FAILED_TO_START, "input-1");

        verify(systemNotificationService).markAsRead(
                eq(Notification.Type.INPUT_FAILED_TO_START),
                eq("input-1"),
                any(SystemNotificationDto.Actor.class));
    }

    @Test
    void fixedByNotificationCallsMarkAsReadByType() {
        final Notification notification = new NotificationBuilder()
                .addType(Notification.Type.ES_UNAVAILABLE);
        adapter.fixed(notification);

        verify(systemNotificationService).markAsRead(eq(Notification.Type.ES_UNAVAILABLE), any(SystemNotificationDto.Actor.class));
    }

    @Test
    void publishIfFirstDelegatesToPublish() {
        when(systemNotificationService.publish(any(), any(), any(), any(), any())).thenReturn(true);
        when(eventPublisher.submit(any())).thenReturn(true);

        final Notification notification = new NotificationBuilder()
                .addType(Notification.Type.ES_UNAVAILABLE)
                .addSeverity(Notification.Severity.URGENT)
                .addNode("test-node-id");
        notification.addDetail("key", "value");

        final boolean result = adapter.publishIfFirst(notification);

        assertThat(result).isTrue();
        verify(systemNotificationService).publish(
                eq(Notification.Type.ES_UNAVAILABLE),
                eq(null),
                eq(Notification.Severity.URGENT),
                eq("test-node-id"),
                eq(Map.of("key", "value")));
        verify(eventPublisher).submit(notification);
    }

    @Test
    void publishIfFirstDoesNotFireEventOnDedupNoOp() {
        when(systemNotificationService.publish(any(), any(), any(), any(), any())).thenReturn(false);

        final Notification notification = new NotificationBuilder()
                .addType(Notification.Type.ES_UNAVAILABLE)
                .addSeverity(Notification.Severity.URGENT)
                .addNode("test-node-id");

        final boolean result = adapter.publishIfFirst(notification);

        assertThat(result).isFalse();
        org.mockito.Mockito.verifyNoInteractions(eventPublisher);
    }

    @Test
    void isFirstReturnsTrueWhenNoUnread() {
        when(systemNotificationService.hasUnread(Notification.Type.ES_UNAVAILABLE)).thenReturn(false);
        assertThat(adapter.isFirst(Notification.Type.ES_UNAVAILABLE)).isTrue();
    }

    @Test
    void isFirstReturnsFalseWhenUnreadExists() {
        when(systemNotificationService.hasUnread(Notification.Type.ES_UNAVAILABLE)).thenReturn(true);
        assertThat(adapter.isFirst(Notification.Type.ES_UNAVAILABLE)).isFalse();
    }

    @Test
    void destroyAllByTypeCallsMarkAsRead() {
        adapter.destroyAllByType(Notification.Type.ES_UNAVAILABLE);
        verify(systemNotificationService).markAsRead(eq(Notification.Type.ES_UNAVAILABLE), any(SystemNotificationDto.Actor.class));
    }

    @Test
    void destroyAllByTypeAndKeyCallsMarkAsReadWithKey() {
        adapter.destroyAllByTypeAndKey(Notification.Type.INPUT_FAILED_TO_START, "input-1");
        verify(systemNotificationService).markAsRead(
                eq(Notification.Type.INPUT_FAILED_TO_START),
                eq("input-1"),
                any(SystemNotificationDto.Actor.class));
    }
}
