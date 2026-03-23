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
package org.graylog2.shared.security;

import org.apache.shiro.subject.Subject;
import org.graylog2.database.DbEntity;
import org.graylog2.database.dbcatalog.DbEntitiesCatalog;
import org.graylog2.database.dbcatalog.DbEntityCatalogEntry;
import org.graylog2.streams.StreamImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class EntityPermissionsUtilsTest {

    @Mock
    private DbEntitiesCatalog catalog;
    @Mock
    private EntityPermissionsUtils toTest;
    @Mock
    private Subject subject;

    @BeforeEach
    void setUp() {
        toTest = new EntityPermissionsUtils(catalog);
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
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, null, List.of()))
        ).when(catalog)
                .getByCollectionName("streams");

        final boolean hasReadPermissions = toTest.hasReadPermissionForWholeCollection(subject, "streams");
        assertFalse(hasReadPermissions);
    }

    @Test
    void hasReadPermissionForWholeCollectionReturnsTrueWhenCatalogHasAllAllowedPermission() {
        doReturn(Optional.of(
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, DbEntity.ALL_ALLOWED, List.of()))
        ).when(catalog)
                .getByCollectionName("streams");

        final boolean hasReadPermissions = toTest.hasReadPermissionForWholeCollection(subject, "streams");
        assertTrue(hasReadPermissions);
    }

    @Test
    void hasReadPermissionForWholeCollectionReturnsFalseWhenSubjectMissesPermission() {
        doReturn(Optional.of(
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read", List.of()))
        ).when(catalog)
                .getByCollectionName("streams");

        doReturn(false).when(subject).isPermitted("streams:read:*");
        final boolean hasReadPermissions = toTest.hasReadPermissionForWholeCollection(subject, "streams");
        assertFalse(hasReadPermissions);
    }

    @Test
    void hasReadPermissionForWholeCollectionReturnsTrueWhenSubjectHasPermission() {
        doReturn(Optional.of(
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read", List.of()))
        ).when(catalog)
                .getByCollectionName("streams");

        doReturn(true).when(subject).isPermitted("streams:read:*");
        final boolean hasReadPermissions = toTest.hasReadPermissionForWholeCollection(subject, "streams");
        assertTrue(hasReadPermissions);
    }

    @Test
    void areFieldsReadableReturnsTrueWhenCollectionNotInCatalog() {
        doReturn(Optional.empty()).when(catalog).getByCollectionName("unknown");

        assertTrue(toTest.areFieldsReadable("unknown", Set.of("any_field")));
    }

    @Test
    void areFieldsReadableReturnsTrueWhenAllFieldsAreReadable() {
        doReturn(Optional.of(
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read",
                        List.of("title", "description", "created_at")))
        ).when(catalog).getByCollectionName("streams");

        assertTrue(toTest.areFieldsReadable("streams", Set.of("title", "description")));
    }

    @Test
    void areFieldsReadableReturnsFalseWhenFieldIsNotInReadableList() {
        doReturn(Optional.of(
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read",
                        List.of("title", "description")))
        ).when(catalog).getByCollectionName("streams");

        assertFalse(toTest.areFieldsReadable("streams", Set.of("title", "secret_field")));
    }

    @Test
    void areFieldsReadableAlwaysAllowsIdField() {
        doReturn(Optional.of(
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read",
                        List.of("title")))
        ).when(catalog).getByCollectionName("streams");

        assertTrue(toTest.areFieldsReadable("streams", Set.of("_id", "title")));
    }

    @Test
    void areFieldsReadableReturnsFalseWhenReadableFieldsIsEmpty() {
        doReturn(Optional.of(
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read",
                        List.of()))
        ).when(catalog).getByCollectionName("streams");

        assertFalse(toTest.areFieldsReadable("streams", Set.of("title")));
    }

    @Test
    void areFieldsReadableAllowsOnlyIdFieldWhenReadableFieldsIsEmpty() {
        doReturn(Optional.of(
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read",
                        List.of()))
        ).when(catalog).getByCollectionName("streams");

        assertTrue(toTest.areFieldsReadable("streams", Set.of("_id")));
    }

}
