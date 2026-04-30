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

import org.graylog.events.processor.systemnotification.SystemNotificationRenderService;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
class SystemNotificationServiceTest {

    @Mock
    private SystemNotificationRenderService renderService;

    private SystemNotificationService service;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        when(renderService.render(any(Notification.class), any(), isNull()))
                .thenReturn(new SystemNotificationRenderService.RenderResponse("Test Title", "Test Description"));
        service = new SystemNotificationService(mongoCollections, renderService);
    }

    @Test
    void publishInsertsNewDocument() {
        final boolean inserted = service.publish(
                Notification.Type.ES_UNAVAILABLE, null, Notification.Severity.URGENT,
                "node-1", Map.of("detail_key", "detail_value"));

        assertThat(inserted).isTrue();

        final var unread = service.findAllUnread();
        assertThat(unread).hasSize(1);
        assertThat(unread.get(0).type()).isEqualTo("es_unavailable");
        assertThat(unread.get(0).severity()).isEqualTo("urgent");
        assertThat(unread.get(0).isRead()).isFalse();
        assertThat(unread.get(0).actor()).isNull();
        assertThat(unread.get(0).lastChanged()).isNull();
        assertThat(unread.get(0).title()).isEqualTo("Test Title");
    }

    @Test
    void publishDedupNoOpWhenUnreadExists() {
        service.publish(Notification.Type.ES_UNAVAILABLE, null, Notification.Severity.URGENT, "node-1", Map.of());

        final boolean second = service.publish(
                Notification.Type.ES_UNAVAILABLE, null, Notification.Severity.URGENT, "node-1", Map.of());

        assertThat(second).isFalse();
        assertThat(service.findAllUnread()).hasSize(1);
    }

    @Test
    void publishWithKeyDedupNoOp() {
        service.publish(Notification.Type.INPUT_FAILED_TO_START, "input-1", Notification.Severity.NORMAL, "node-1", Map.of());

        final boolean second = service.publish(
                Notification.Type.INPUT_FAILED_TO_START, "input-1", Notification.Severity.NORMAL, "node-1", Map.of());

        assertThat(second).isFalse();
        assertThat(service.findAllUnread()).hasSize(1);
    }

    @Test
    void publishCreatesNewDocumentAfterMarkAsRead() {
        service.publish(Notification.Type.ES_UNAVAILABLE, null, Notification.Severity.URGENT, "node-1", Map.of());
        service.markAsRead(Notification.Type.ES_UNAVAILABLE, SystemNotificationDto.Actor.system());

        // Re-occurrence: should create a new document
        final boolean inserted = service.publish(
                Notification.Type.ES_UNAVAILABLE, null, Notification.Severity.URGENT, "node-1", Map.of());

        assertThat(inserted).isTrue();
        assertThat(service.findAllUnread()).hasSize(1);
    }

    @Test
    void markAsReadSetsActorAndTimestamp() {
        service.publish(Notification.Type.ES_UNAVAILABLE, null, Notification.Severity.URGENT, "node-1", Map.of());
        final var actor = SystemNotificationDto.Actor.create("user-1", "Jane Doe");

        service.markAsRead(Notification.Type.ES_UNAVAILABLE, actor);

        final var all = service.findByTypeAndKey(Notification.Type.ES_UNAVAILABLE, null);
        assertThat(all).isPresent();
        assertThat(all.get().isRead()).isTrue();
        assertThat(all.get().actor()).isNotNull();
        assertThat(all.get().actor().id()).isEqualTo("user-1");
        assertThat(all.get().actor().name()).isEqualTo("Jane Doe");
        assertThat(all.get().lastChanged()).isNotNull();
    }

    @Test
    void markAsReadByTypeAndKey() {
        service.publish(Notification.Type.INPUT_FAILED_TO_START, "input-1", Notification.Severity.NORMAL, "node-1", Map.of());
        service.publish(Notification.Type.INPUT_FAILED_TO_START, "input-2", Notification.Severity.NORMAL, "node-1", Map.of());

        service.markAsRead(Notification.Type.INPUT_FAILED_TO_START, "input-1", SystemNotificationDto.Actor.system());

        final var unread = service.findAllUnread();
        assertThat(unread).hasSize(1);
        assertThat(unread.get(0).key()).isEqualTo("input-2");
    }

    @Test
    void markAsUnread() {
        service.publish(Notification.Type.ES_UNAVAILABLE, null, Notification.Severity.URGENT, "node-1", Map.of());
        final String id = service.findAllUnread().get(0).id();

        service.markAsRead(List.of(id), SystemNotificationDto.Actor.system());
        assertThat(service.findAllUnread()).isEmpty();

        final var actor = SystemNotificationDto.Actor.create("user-1", "Jane Doe");
        service.markAsUnread(List.of(id), actor);

        final var unread = service.findAllUnread();
        assertThat(unread).hasSize(1);
        assertThat(unread.get(0).actor().id()).isEqualTo("user-1");
    }

    @Test
    void markAllAsRead() {
        service.publish(Notification.Type.ES_UNAVAILABLE, null, Notification.Severity.URGENT, "node-1", Map.of());
        service.publish(Notification.Type.INPUT_FAILED_TO_START, "input-1", Notification.Severity.NORMAL, "node-1", Map.of());

        assertThat(service.findAllUnread()).hasSize(2);

        service.markAllAsRead(SystemNotificationDto.Actor.system());

        assertThat(service.findAllUnread()).isEmpty();
    }

    @Test
    void toggleReadState() {
        service.publish(Notification.Type.ES_UNAVAILABLE, null, Notification.Severity.URGENT, "node-1", Map.of());
        service.publish(Notification.Type.INPUT_FAILED_TO_START, "input-1", Notification.Severity.NORMAL, "node-1", Map.of());

        final var all = service.findAllUnread();
        final String id1 = all.get(0).id();
        final String id2 = all.get(1).id();

        // Mark first as read
        service.markAsRead(List.of(id1), SystemNotificationDto.Actor.system());

        // Toggle both: id1 (read->unread), id2 (unread->read)
        final var actor = SystemNotificationDto.Actor.create("user-1", "Jane Doe");
        service.toggleReadState(List.of(id1, id2), actor);

        final var dto1 = service.findById(id1).orElseThrow();
        final var dto2 = service.findById(id2).orElseThrow();

        assertThat(dto1.isRead()).isFalse();
        assertThat(dto2.isRead()).isTrue();
    }

    @Test
    void deleteOlderThanRemovesOnlyReadEntries() {
        service.publish(Notification.Type.ES_UNAVAILABLE, null, Notification.Severity.URGENT, "node-1", Map.of());
        service.publish(Notification.Type.INPUT_FAILED_TO_START, "input-1", Notification.Severity.NORMAL, "node-1", Map.of());

        // Mark one as read
        final String readId = service.findAllUnread().get(0).id();
        service.markAsRead(List.of(readId), SystemNotificationDto.Actor.system());

        // Delete entries older than a future cutoff (should catch the read one)
        final long deleted = service.deleteOlderThan(Instant.now().plus(1, ChronoUnit.HOURS));

        assertThat(deleted).isEqualTo(1);
        assertThat(service.findAllUnread()).hasSize(1);
    }

    @Test
    void hasUnread() {
        assertThat(service.hasUnread(Notification.Type.ES_UNAVAILABLE)).isFalse();

        service.publish(Notification.Type.ES_UNAVAILABLE, null, Notification.Severity.URGENT, "node-1", Map.of());
        assertThat(service.hasUnread(Notification.Type.ES_UNAVAILABLE)).isTrue();

        service.markAsRead(Notification.Type.ES_UNAVAILABLE, SystemNotificationDto.Actor.system());
        assertThat(service.hasUnread(Notification.Type.ES_UNAVAILABLE)).isFalse();
    }

    @Test
    void findByTypeAndKeyPrefersUnread() {
        // Insert, mark as read, insert again (re-occurrence)
        service.publish(Notification.Type.ES_UNAVAILABLE, null, Notification.Severity.URGENT, "node-1", Map.of());
        service.markAsRead(Notification.Type.ES_UNAVAILABLE, SystemNotificationDto.Actor.system());
        service.publish(Notification.Type.ES_UNAVAILABLE, null, Notification.Severity.URGENT, "node-1", Map.of());

        final var result = service.findByTypeAndKey(Notification.Type.ES_UNAVAILABLE, null);
        assertThat(result).isPresent();
        assertThat(result.get().isRead()).isFalse();
    }
}
