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
package org.graylog2.database.export;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.apache.shiro.subject.Subject;
import org.bson.Document;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.database.MongoConnection;
import org.graylog2.shared.security.EntityPermissionsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;


@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(MockitoExtension.class)
class MongoCollectionExportServiceTest {

    private static final String TEST_COLLECTION_NAME = "people";
    @Mock
    private EntityPermissionsUtils permissionsUtils;
    @Mock
    private Subject subject;
    private MongoCollection<Document> collection;
    private MongoCollectionExportService toTest;

    @BeforeEach
    void setUp(MongoDBTestService mongoDBTestService) {
        final MongoConnection mongoConnection = mongoDBTestService.mongoConnection();
        toTest = new MongoCollectionExportService(mongoConnection, permissionsUtils);
        collection = mongoConnection.getMongoDatabase().getCollection(TEST_COLLECTION_NAME);
        collection.deleteMany(Filters.empty());
    }

    @Test
    void testExportUsesProjectionCorrectly() {
        insertTestData();
        simulateAdminUser();
        final List<Document> exportedDocuments = toTest.export(TEST_COLLECTION_NAME,
                List.of("name"),
                10,
                Filters.empty(),
                List.of(),
                subject);

        assertThat(exportedDocuments)
                .isNotNull()
                .hasSize(3)
                .containsExactlyInAnyOrder(
                        new Document(Map.of("_id", "0000000000000000000000a5", "name", "John")),
                        new Document(Map.of("_id", "0000000000000000000000b6", "name", "Jerry")),
                        new Document(Map.of("_id", "0000000000000000000000c7", "name", "Judith"))
                );

    }

    @Test
    void testExportUsesSortAndLimitCorrectly() {
        insertTestData();
        simulateAdminUser();
        final List<Document> exportedDocuments = toTest.export(TEST_COLLECTION_NAME,
                List.of("name"),
                2,
                Filters.empty(),
                List.of(Sort.create("age", Sort.Order.ASC)),
                subject);

        assertThat(exportedDocuments)
                .isNotNull()
                .hasSize(2)
                .containsExactly(
                        new Document(Map.of("_id", "0000000000000000000000c7", "name", "Judith")),
                        new Document(Map.of("_id", "0000000000000000000000b6", "name", "Jerry"))
                );

    }

    @Test
    void testExportUsesFilterCorrectly() {
        insertTestData();
        simulateAdminUser();
        final List<Document> exportedDocuments = toTest.export(TEST_COLLECTION_NAME,
                List.of("name"),
                200,
                Filters.gt("age", 40),
                List.of(),
                subject);

        assertThat(exportedDocuments)
                .isNotNull()
                .hasSize(1)
                .containsExactly(
                        new Document(Map.of("_id", "0000000000000000000000a5", "name", "John"))
                );
    }

    @Test
    void testExportWorksCorrectlyWithSelectivePermissions() {
        insertTestData();
        simulateUserThatCanSeeOnlyOneDoc("0000000000000000000000c7");

        final List<Document> exportedDocuments = toTest.export(TEST_COLLECTION_NAME,
                List.of("name"),
                10,
                Filters.empty(),
                List.of(),
                subject);

        assertThat(exportedDocuments)
                .isNotNull()
                .hasSize(1)
                .containsExactlyInAnyOrder(
                        new Document(Map.of("_id", "0000000000000000000000c7", "name", "Judith"))
                );

    }

    private void insertTestData() {
        collection.insertOne(new Document(Map.of("_id", "0000000000000000000000a5", "name", "John", "age", 42)));
        collection.insertOne(new Document(Map.of("_id", "0000000000000000000000b6", "name", "Jerry", "age", 32)));
        collection.insertOne(new Document(Map.of("_id", "0000000000000000000000c7", "name", "Judith", "age", 22)));
    }

    private void simulateAdminUser() {
        doReturn(true).when(permissionsUtils).hasAllPermission(subject);
    }

    private void simulateUserThatCanSeeOnlyOneDoc(final String docId) {
        doReturn(false).when(permissionsUtils).hasAllPermission(subject);
        doReturn(false).when(permissionsUtils).hasReadPermissionForWholeCollection(subject, TEST_COLLECTION_NAME);
        doReturn((Predicate<Document>) document -> docId.equals(document.get(EntityPermissionsUtils.ID_FIELD).toString()))
                .when(permissionsUtils)
                .createPermissionCheck(subject, TEST_COLLECTION_NAME);
    }
}
