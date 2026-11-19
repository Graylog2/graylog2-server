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
package org.graylog2.database.aggregation;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.Subject;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.database.DbEntity;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.dbcatalog.DbEntitiesCatalog;
import org.graylog2.database.dbcatalog.DbEntityCatalogEntry;
import org.graylog2.shared.security.EntityPermissionsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Builds permission filters for the simplified Security app data described in {@link MongoAggregatorScenariosTest}.
 */
@ExtendWith(MongoDBExtension.class)
@MongoDBFixtures("security-events.json")
class CatalogPermissionFiltersTest {
    private static final String WEB_01 = "65f0000000000000000000a1";
    private static final String LAPTOP_07 = "65f0000000000000000000a3";

    private final Set<String> permissions = new HashSet<>();
    private final Subject subject = mock(Subject.class);
    private MongoDBTestService mongodb;
    private CatalogPermissionFilters permissionFilters;

    @BeforeEach
    void setUp(final MongoCollections mongoCollections, final MongoDBTestService mongodb) {
        this.mongodb = mongodb;
        final var catalog = new DbEntitiesCatalog(List.of(
                new DbEntityCatalogEntry("assets", "name", Object.class, "assets:read", List.of()),
                new DbEntityCatalogEntry("users", "full_name", Object.class, DbEntity.ALL_ALLOWED, List.of()),
                new DbEntityCatalogEntry("security_events", "event_id", Object.class, DbEntity.NOBODY_ALLOWED,
                        List.of())));
        permissionFilters = new CatalogPermissionFilters(mongoCollections.connection(),
                new EntityPermissionsUtils(catalog));
        when(subject.isPermitted(anyString()))
                .thenAnswer(invocation -> permissions.contains(invocation.<String>getArgument(0)));
    }

    @Test
    void readsOnlyTheEntitiesTheUserIsPermittedFor() {
        permissions.addAll(List.of("assets:read:" + WEB_01, "assets:read:" + LAPTOP_07));

        final var permissionFilter = permissionFilters.permissionFilter("assets", subject);

        assertThat(readableNames("assets", permissionFilter)).containsExactlyInAnyOrder("web-01", "laptop-07");
    }

    @Test
    void readsNothingWithoutPermissions() {
        final var permissionFilter = permissionFilters.permissionFilter("assets", subject);

        assertThat(readableNames("assets", permissionFilter)).isEmpty();
    }

    @Test
    void readsTheWholeCollectionWithAWildcardPermission() {
        permissions.add("assets:read:*");

        assertThat(permissionFilters.permissionFilter("assets", subject)).isEqualTo(PermissionFilter.unrestricted());
    }

    @Test
    void readsEverythingAsAdmin() {
        when(subject.isPermitted(any(Permission.class))).thenReturn(true);

        assertThat(permissionFilters.permissionFilter("assets", subject)).isEqualTo(PermissionFilter.unrestricted());
        assertThat(permissionFilters.permissionFilter("security_events", subject))
                .isEqualTo(PermissionFilter.unrestricted());
    }

    @Test
    void readsCollectionsOpenToEveryone() {
        assertThat(permissionFilters.permissionFilter("users", subject)).isEqualTo(PermissionFilter.unrestricted());
    }

    @Test
    void readsNothingOfCollectionsClosedToEveryone() {
        permissions.add("security_events:read:*");

        final var permissionFilter = permissionFilters.permissionFilter("security_events", subject);

        assertThat(mongodb.mongoCollection("security_events").countDocuments(permissionFilter.filter())).isZero();
    }

    @Test
    void rejectsCollectionsMissingFromTheCatalog() {
        assertThatThrownBy(() -> permissionFilters.permissionFilter("streams", subject))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Collection <streams> isn't registered");
    }

    private List<String> readableNames(final String collection, final PermissionFilter permissionFilter) {
        return mongodb.mongoCollection(collection).find(permissionFilter.filter())
                .map(document -> document.getString("name"))
                .into(new ArrayList<>());
    }
}
