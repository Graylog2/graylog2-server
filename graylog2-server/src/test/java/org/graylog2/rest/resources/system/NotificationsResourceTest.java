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

import jakarta.ws.rs.NotFoundException;
import org.graylog.events.processor.systemnotification.SystemNotificationRenderService;
import org.graylog2.database.PaginatedList;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationPaginationService;
import org.graylog2.notifications.NotificationService;
import org.graylog2.notifications.NotificationSummaryDto;
import org.graylog2.rest.bulk.model.BulkOperationRequest;
import org.graylog2.rest.models.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationsResourceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationPaginationService paginationService;

    @Mock
    private SystemNotificationRenderService renderService;

    private TestResource resource;
    private TestResource cloudResource;

    @BeforeEach
    void setUp() {
        resource = new TestResource(notificationService, paginationService, renderService, false);
        cloudResource = new TestResource(notificationService, paginationService, renderService, true);
    }

    // ---- Legacy endpoint tests ----

    @Test
    void cloudModeSuppressesSuppressedTypesAndKeepsOthers() {
        assertThat(Notification.CLOUD_SUPPRESSED_TYPES).contains(Notification.Type.ES_CLUSTER_RED);

        when(notificationService.all()).thenReturn(List.of(
                new NotificationImpl().addType(Notification.Type.ES_CLUSTER_RED),
                new NotificationImpl().addType(Notification.Type.SEARCH_ERROR)
        ));

        final var response = cloudResource.listNotifications();

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

        final var response = resource.listNotifications();

        assertThat(response.notifications()).hasSize(2);
    }

    // ---- Paginated endpoint tests ----

    @Test
    void getPaginatedDelegatesToPaginationService() {
        final var dtos = List.of(
                new NotificationSummaryDto("id1", "es_unavailable", null, "urgent", "node-1",
                        "Title", "Desc", Map.of(), "2026-01-01T00:00:00.000Z")
        );
        when(paginationService.searchPaginated(any(), any(), eq(1), eq(50)))
                .thenReturn(new PaginatedList<>(dtos, 1, 1, 50));

        final var response = resource.getPaginated(1, 50, "", List.of(), "timestamp", SortOrder.DESCENDING);

        assertThat(response.elements()).hasSize(1);
        assertThat(response.elements().get(0).type()).isEqualTo("es_unavailable");
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.attributes()).isNotEmpty();
        assertThat(response.defaults()).isNotNull();
    }

    @Test
    void getPaginatedPassesSortOrder() {
        when(paginationService.searchPaginated(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PaginatedList<>(List.of(), 0, 1, 10));

        final var response = resource.getPaginated(1, 10, "", List.of(), "timestamp", SortOrder.ASCENDING);

        assertThat(response.order()).isEqualTo(SortOrder.ASCENDING);
    }

    // ---- Count endpoint tests ----

    @Test
    void getCountDelegatesToPaginationService() {
        when(paginationService.count(any())).thenReturn(42L);

        assertThat(resource.getCount()).isEqualTo(42L);
    }

    @Test
    void getCountInCloudModePassesSuppressionFilter() {
        when(paginationService.count(any())).thenReturn(5L);

        assertThat(cloudResource.getCount()).isEqualTo(5L);
        verify(paginationService).count(any());
    }

    // ---- Delete by ID tests ----

    @Test
    void deleteNotificationByIdCallsService() {
        when(paginationService.deleteById("abc123")).thenReturn(true);

        resource.deleteNotificationById("abc123");

        verify(paginationService).deleteById("abc123");
    }

    @Test
    void deleteNotificationByIdThrows404WhenNotFound() {
        when(paginationService.deleteById("missing")).thenReturn(false);

        assertThatThrownBy(() -> resource.deleteNotificationById("missing"))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- Bulk delete tests ----

    @Test
    void bulkDeleteDelegatesToPaginationService() {
        final var request = new BulkOperationRequest(List.of("id1", "id2", "id3"));
        when(paginationService.bulkDelete(any())).thenReturn(3L);

        final var response = resource.bulkDelete(request);

        assertThat(response.getStatus()).isEqualTo(204);
        final var captor = ArgumentCaptor.forClass(List.class);
        verify(paginationService).bulkDelete(captor.capture());
        assertThat(captor.getValue()).containsExactly("id1", "id2", "id3");
    }

    // ---- Helper ----

    static class TestResource extends NotificationsResource {
        TestResource(NotificationService notificationService, NotificationPaginationService paginationService,
                     SystemNotificationRenderService renderService, boolean isCloud) {
            super(notificationService, paginationService, renderService, isCloud);
        }

        @Override
        protected boolean isPermitted(String permission, String instanceId) {
            return true;
        }

        @Override
        protected boolean isPermitted(String permission) {
            return true;
        }

        @Override
        protected void checkPermission(String permission) {
            // no-op for tests
        }

        @Override
        protected void checkPermission(String permission, String instanceId) {
            // no-op for tests
        }
    }
}
