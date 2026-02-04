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
package org.graylog2.database.suggestions;

import org.apache.shiro.subject.Subject;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.shared.security.EntityPermissionsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
@MongoDBFixtures("simple-fixtures.json")
class MongoEntitySuggestionServiceTest {
    @Mock
    private EntityPermissionsUtils entityPermissionsUtils;
    @Mock
    private Subject subject;

    private MongoEntitySuggestionService toTest;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        this.toTest = new MongoEntitySuggestionService(mongodb.mongoConnection(), entityPermissionsUtils);
    }

    @Test
    void checksPermissionsForEachDocumentWhenUserDoesNotHavePermissionForWholeCollection() {
        doReturn(false).when(entityPermissionsUtils).hasAllPermission(subject);
        doReturn(false).when(entityPermissionsUtils).hasReadPermissionForWholeCollection(subject, "dashboards");

        final Collection<String> permittedIds = List.of("5a82f5974b900a7a97caa1e5", "5a82f5974b900a7a97caa1e7");
        doReturn((Predicate<Document>) document -> permittedIds.contains(document.getObjectId(EntityPermissionsUtils.ID_FIELD).toString()))
                .when(entityPermissionsUtils)
                .createPermissionCheck(subject, "dashboards");

        final var result = toTest.suggest("dashboards", "_id", "title", "", 1, 10, subject);

        assertThat(result.pagination().count()).isEqualTo(2);

        final var suggestions = result.suggestions();
        assertThat(suggestions).hasSize(2);
        assertThat(suggestions.stream().map(EntitySuggestion::id).toList())
                .containsExactlyInAnyOrder("5a82f5974b900a7a97caa1e5", "5a82f5974b900a7a97caa1e7");
        assertThat(suggestions.stream().map(EntitySuggestion::value).toList())
                .containsExactlyInAnyOrder("Test", "Test 3");
    }

    @Test
    @MongoDBFixtures("composite-display-fixtures.json")
    void testCompositeDisplayWithTemplate() {
        doReturn(true).when(entityPermissionsUtils).hasAllPermission(subject);

        final var result = toTest.suggest(
                "nodes",
                "node_id",
                "hostname",
                List.of("node_id", "hostname"),
                "{node_id} ({hostname})",
                "",
                1,
                10,
                subject
        );

        assertThat(result.pagination().count()).isEqualTo(4);

        final var suggestions = result.suggestions();
        assertThat(suggestions).hasSize(4);

        final var suggestion1 = suggestions.stream()
                .filter(s -> s.targetId().equals("node-uuid-123"))
                .findFirst()
                .orElseThrow();
        assertThat(suggestion1.value()).isEqualTo("node-uuid-123 (webserver1)");

        final var suggestion2 = suggestions.stream()
                .filter(s -> s.targetId().equals("node-uuid-456"))
                .findFirst()
                .orElseThrow();
        assertThat(suggestion2.value()).isEqualTo("node-uuid-456 (webserver2)");

        final var suggestion3 = suggestions.stream()
                .filter(s -> s.targetId().equals("node-uuid-789"))
                .findFirst()
                .orElseThrow();
        assertThat(suggestion3.value()).isEqualTo("node-uuid-789");
    }

    @Test
    @MongoDBFixtures("composite-display-fixtures.json")
    void testCompositeDisplayWithoutTemplate() {
        doReturn(true).when(entityPermissionsUtils).hasAllPermission(subject);

        final var result = toTest.suggest(
                "nodes",
                "node_id",
                "hostname",
                List.of("node_id", "hostname"),
                null,
                "",
                1,
                10,
                subject
        );

        assertThat(result.pagination().count()).isEqualTo(4);

        final var suggestions = result.suggestions();
        assertThat(suggestions).hasSize(4);

        final var suggestion1 = suggestions.stream()
                .filter(s -> s.targetId().equals("node-uuid-123"))
                .findFirst()
                .orElseThrow();
        assertThat(suggestion1.value()).isEqualTo("node-uuid-123 webserver1");
    }

    @Test
    @MongoDBFixtures("composite-display-fixtures.json")
    void testCompositeDisplayFilterByValue() {
        doReturn(true).when(entityPermissionsUtils).hasAllPermission(subject);

        final var result = toTest.suggest(
                "nodes",
                "node_id",
                "hostname",
                List.of("node_id", "hostname"),
                "{node_id} ({hostname})",
                "webserver1",
                1,
                10,
                subject
        );

        assertThat(result.pagination().count()).isEqualTo(1);

        final var suggestions = result.suggestions();
        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).targetId()).isEqualTo("node-uuid-123");
        assertThat(suggestions.get(0).value()).isEqualTo("node-uuid-123 (webserver1)");
    }

    @Test
    @MongoDBFixtures("composite-display-fixtures.json")
    void testBackwardCompatibilityWithoutDisplayFields() {
        doReturn(true).when(entityPermissionsUtils).hasAllPermission(subject);

        // Test without display fields - should use legacy valueColumn
        final var result = toTest.suggest(
                "nodes",
                "node_id",
                "hostname",
                null,
                null,
                "",
                1,
                10,
                subject
        );

        assertThat(result.pagination().count()).isEqualTo(4);

        final var suggestions = result.suggestions();
        assertThat(suggestions).hasSize(4);

        final var suggestion1 = suggestions.stream()
                .filter(s -> s.targetId().equals("node-uuid-123"))
                .findFirst()
                .orElseThrow();
        assertThat(suggestion1.value()).isEqualTo("webserver1");
    }

    @Test
    @MongoDBFixtures("composite-display-fixtures.json")
    void testTargetIdIsAlwaysPresent() {
        doReturn(true).when(entityPermissionsUtils).hasAllPermission(subject);

        final var result = toTest.suggest(
                "nodes",
                "node_id",
                "hostname",
                List.of("node_id", "hostname"),
                "{node_id} ({hostname})",
                "",
                1,
                10,
                subject
        );

        final var suggestions = result.suggestions();
        assertThat(suggestions).allMatch(s -> s.targetId() != null && !s.targetId().isEmpty());
        assertThat(suggestions.stream().map(EntitySuggestion::targetId).toList())
                .containsExactlyInAnyOrder("node-uuid-123", "node-uuid-456", "node-uuid-789", "node-uuid-abc");
    }
}
