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

import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.subject.Subject;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.database.DbEntity;
import org.graylog2.database.dbcatalog.DbEntitiesCatalog;
import org.graylog2.database.dbcatalog.DbEntityCatalogEntry;
import org.graylog2.streams.StreamImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
@MongoDBFixtures("simple-fixtures.json")
class MongoEntitySuggestionServiceTest {
    @Mock
    private DbEntitiesCatalog catalog;
    @Mock
    private Subject subject;

    private MongoEntitySuggestionService toTest;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        this.toTest = new MongoEntitySuggestionService(mongodb.mongoConnection(), catalog);
    }

    @Test
    void hasReadPermissionForWholeCollectionReturnsFalseOnNoEntryInCatalog() {
        doReturn(Optional.empty()).when(catalog).getByCollectionName("streams");

        final boolean hasReadPermissions = toTest.hasReadPermissionForWholeCollection(subject, "streams");
        assertFalse(hasReadPermissions);
    }

    @Test
    void hasReadPermissionForWholeCollectionReturnsFalseWhenCatalogHasNullPermission() {
        doReturn(Optional.of(
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, null))
        ).when(catalog)
                .getByCollectionName("streams");

        final boolean hasReadPermissions = toTest.hasReadPermissionForWholeCollection(subject, "streams");
        assertFalse(hasReadPermissions);
    }

    @Test
    void hasReadPermissionForWholeCollectionReturnsTrueWhenCatalogHasAllAllowedPermission() {
        doReturn(Optional.of(
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, DbEntity.ALL_ALLOWED))
        ).when(catalog)
                .getByCollectionName("streams");

        final boolean hasReadPermissions = toTest.hasReadPermissionForWholeCollection(subject, "streams");
        assertTrue(hasReadPermissions);
    }

    @Test
    void hasReadPermissionForWholeCollectionReturnsFalseWhenSubjectMissesPermission() {
        doReturn(Optional.of(
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read"))
        ).when(catalog)
                .getByCollectionName("streams");

        doReturn(false).when(subject).isPermitted("streams:read:*");
        final boolean hasReadPermissions = toTest.hasReadPermissionForWholeCollection(subject, "streams");
        assertFalse(hasReadPermissions);
    }

    @Test
    void hasReadPermissionForWholeCollectionReturnsTrueWhenSubjectHasPermission() {
        doReturn(Optional.of(
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read"))
        ).when(catalog)
                .getByCollectionName("streams");

        doReturn(true).when(subject).isPermitted("streams:read:*");
        final boolean hasReadPermissions = toTest.hasReadPermissionForWholeCollection(subject, "streams");
        assertTrue(hasReadPermissions);
    }

    private record Dashboard(String id, String title) {}

    @Test
    void checksPermissionsForEachDocumentWhenUserDoesNotHavePermissionForWholeCollection() {
        final var permission = "dashboard:read";
        doReturn(Optional.of(
                new DbEntityCatalogEntry("dashboards", "title", Dashboard.class, permission))
        ).when(catalog)
                .getByCollectionName("dashboards");

        doReturn(false).when(subject).isPermitted(permission + ":*");
        when(subject.isPermitted(any(AllPermission.class))).thenReturn(false);
        doReturn(true).when(subject).isPermitted(permission + ":5a82f5974b900a7a97caa1e5");
        doReturn(false).when(subject).isPermitted(permission + ":5a82f5974b900a7a97caa1e6");
        doReturn(true).when(subject).isPermitted(permission + ":5a82f5974b900a7a97caa1e7");

        final var result = toTest.suggest("dashboards", "title", "", 1, 10, subject);
        final var suggestions = result.suggestions();

        assertThat(suggestions).hasSize(2);
        assertThat(suggestions.stream().map(EntitySuggestion::id).toList())
                .containsExactlyInAnyOrder("5a82f5974b900a7a97caa1e5", "5a82f5974b900a7a97caa1e7");
        assertThat(suggestions.stream().map(EntitySuggestion::value).toList())
                .containsExactlyInAnyOrder("Test", "Test 3");
    }
}
