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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.events.processor.systemnotification.SystemNotificationRenderService;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
class NotificationPaginationServiceTest {

    private final MongoCollection<Document> collection;
    private final NotificationPaginationService service;
    private final SystemNotificationRenderService renderService;

    NotificationPaginationServiceTest(MongoDBTestService mongodb, @Mock SystemNotificationRenderService renderService) {
        this.collection = mongodb.mongoCollection("notifications");
        this.renderService = renderService;
        this.service = new NotificationPaginationService(mongodb.mongoConnection(), renderService);
    }

    @BeforeEach
    void setUp() {
        collection.drop();
    }

    @Test
    void searchPaginatedReturnsEmptyListWhenNoDocuments() {
        final var result = service.searchPaginated(Filters.empty(), Sorts.descending("timestamp"), 1, 10);

        assertThat(result).isEmpty();
        assertThat(result.pagination().total()).isZero();
    }

    @Test
    void searchPaginatedReturnsSortedResults() {
        stubRenderService("Rendered Title", "Rendered Description");
        insertNotification("es_unavailable", null, "urgent", "node-1", "2026-01-01T00:00:00.000Z");
        insertNotification("no_input_running", null, "normal", "node-2", "2026-01-02T00:00:00.000Z");
        insertNotification("input_failing", "input-1", "normal", "node-1", "2026-01-03T00:00:00.000Z");

        final var result = service.searchPaginated(Filters.empty(), Sorts.descending("timestamp"), 1, 10);

        assertThat(result).hasSize(3);
        assertThat(result.pagination().total()).isEqualTo(3);
        assertThat(result.get(0).type()).isEqualTo("input_failing");
        assertThat(result.get(1).type()).isEqualTo("no_input_running");
        assertThat(result.get(2).type()).isEqualTo("es_unavailable");
    }

    @Test
    void searchPaginatedRespectsPageAndPerPage() {
        stubRenderService("Title", "Desc");
        for (int i = 0; i < 5; i++) {
            insertNotification("type_" + i, null, "normal", "node-1",
                    Tools.getISO8601String(DateTime.now(DateTimeZone.UTC).plusMinutes(i)));
        }

        final var page1 = service.searchPaginated(Filters.empty(), Sorts.descending("timestamp"), 1, 2);
        assertThat(page1).hasSize(2);
        assertThat(page1.pagination().total()).isEqualTo(5);
        assertThat(page1.pagination().page()).isEqualTo(1);

        final var page2 = service.searchPaginated(Filters.empty(), Sorts.descending("timestamp"), 2, 2);
        assertThat(page2).hasSize(2);
        assertThat(page2.pagination().page()).isEqualTo(2);

        final var page3 = service.searchPaginated(Filters.empty(), Sorts.descending("timestamp"), 3, 2);
        assertThat(page3).hasSize(1);
    }

    @Test
    void searchPaginatedAppliesFilter() {
        stubRenderService("Title", "Desc");
        insertNotification("es_unavailable", null, "urgent", "node-1", "2026-01-01T00:00:00.000Z");
        insertNotification("no_input_running", null, "normal", "node-2", "2026-01-02T00:00:00.000Z");

        final var result = service.searchPaginated(
                Filters.eq("type", "es_unavailable"), Sorts.descending("timestamp"), 1, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo("es_unavailable");
        assertThat(result.get(0).severity()).isEqualTo("urgent");
    }

    @Test
    void searchPaginatedPopulatesAllDtoFields() {
        stubRenderService("My Title", "My Description");
        final String ts = "2026-05-19T10:00:00.000Z";
        insertNotification("input_failing", "input-42", "normal", "node-abc", ts,
                Map.of("input_id", "input-42", "reason", "connection refused"));

        final var result = service.searchPaginated(Filters.empty(), Sorts.descending("timestamp"), 1, 10);

        assertThat(result).hasSize(1);
        final var dto = result.get(0);
        assertThat(dto.id()).isNotBlank();
        assertThat(dto.type()).isEqualTo("input_failing");
        assertThat(dto.key()).isEqualTo("input-42");
        assertThat(dto.severity()).isEqualTo("normal");
        assertThat(dto.nodeId()).isEqualTo("node-abc");
        assertThat(dto.title()).isEqualTo("My Title");
        assertThat(dto.description()).isEqualTo("My Description");
        assertThat(dto.timestamp()).isEqualTo(ts);
        assertThat(dto.details()).containsEntry("input_id", "input-42");
    }

    @Test
    void searchPaginatedHandlesRenderFailureGracefully() {
        when(renderService.render(any(Notification.class))).thenThrow(new RuntimeException("template missing"));
        insertNotification("es_unavailable", null, "urgent", "node-1", "2026-01-01T00:00:00.000Z");

        final var result = service.searchPaginated(Filters.empty(), Sorts.descending("timestamp"), 1, 10);

        assertThat(result).hasSize(1);
        final var dto = result.get(0);
        assertThat(dto.type()).isEqualTo("es_unavailable");
        assertThat(dto.title()).isNull();
        assertThat(dto.description()).isNull();
    }

    @Test
    void deleteByIdRemovesDocument() {
        final ObjectId id = insertNotification("es_unavailable", null, "urgent", "node-1", "2026-01-01T00:00:00.000Z");

        assertThat(service.deleteById(id.toHexString())).isTrue();
        assertThat(collection.countDocuments()).isZero();
    }

    @Test
    void deleteByIdReturnsFalseForMissingDocument() {
        assertThat(service.deleteById(new ObjectId().toHexString())).isFalse();
    }

    @Test
    void bulkDeleteRemovesMultipleDocuments() {
        final ObjectId id1 = insertNotification("es_unavailable", null, "urgent", "node-1", "2026-01-01T00:00:00.000Z");
        final ObjectId id2 = insertNotification("no_input_running", null, "normal", "node-2", "2026-01-02T00:00:00.000Z");
        insertNotification("input_failing", null, "normal", "node-1", "2026-01-03T00:00:00.000Z");

        final long deleted = service.bulkDelete(List.of(id1.toHexString(), id2.toHexString()));

        assertThat(deleted).isEqualTo(2);
        assertThat(collection.countDocuments()).isEqualTo(1);
    }

    @Test
    void countReturnsDocumentCount() {
        assertThat(service.count(Filters.empty())).isZero();

        insertNotification("es_unavailable", null, "urgent", "node-1", "2026-01-01T00:00:00.000Z");
        insertNotification("no_input_running", null, "normal", "node-2", "2026-01-02T00:00:00.000Z");

        assertThat(service.count(Filters.empty())).isEqualTo(2);
    }

    @Test
    void countRespectsFilter() {
        insertNotification("es_unavailable", null, "urgent", "node-1", "2026-01-01T00:00:00.000Z");
        insertNotification("no_input_running", null, "normal", "node-2", "2026-01-02T00:00:00.000Z");
        insertNotification("input_failing", null, "normal", "node-1", "2026-01-03T00:00:00.000Z");

        assertThat(service.count(Filters.nin("type", "es_unavailable"))).isEqualTo(2);
    }

    // ---- Helpers ----

    private void stubRenderService(String title, String description) {
        // RenderResponse is a non-static inner class, so we create it through the mocked instance
        final var response = renderService.new RenderResponse(title, description);
        when(renderService.render(any(Notification.class))).thenReturn(response);
    }

    private ObjectId insertNotification(String type, String key, String severity, String nodeId, String timestamp) {
        return insertNotification(type, key, severity, nodeId, timestamp, Map.of());
    }

    private ObjectId insertNotification(String type, String key, String severity, String nodeId, String timestamp,
                                        Map<String, Object> details) {
        final var doc = new Document()
                .append("type", type)
                .append("severity", severity)
                .append("node_id", nodeId)
                .append("timestamp", timestamp);
        if (key != null) {
            doc.append("key", key);
        }
        if (!details.isEmpty()) {
            doc.append("details", new Document(details));
        }
        collection.insertOne(doc);
        return doc.getObjectId("_id");
    }
}
