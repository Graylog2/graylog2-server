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
package org.graylog2.database.slicing;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.Subject;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.database.DbEntity;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.dbcatalog.DbEntitiesCatalog;
import org.graylog2.database.dbcatalog.DbEntityCatalogEntry;
import org.graylog2.database.filtering.DbQueryCreator;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.Slice;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntitiesTitleResponse;
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
 * Builds read scopes for the simplified Security app data described in
 * {@link org.graylog2.database.aggregation.MongoAggregatorScenariosTest}.
 */
@ExtendWith(MongoDBExtension.class)
@MongoDBFixtures("org/graylog2/database/aggregation/security-events.json")
class CatalogReadScopesTest {
    private static final String WEB_01 = "65f0000000000000000000a1";
    private static final String DB_01 = "65f0000000000000000000a2";
    private static final String LAPTOP_07 = "65f0000000000000000000a3";

    private final Set<String> permissions = new HashSet<>();
    private final Subject subject = mock(Subject.class);
    private MongoCollections mongoCollections;
    private MongoDBTestService mongodb;
    private CatalogReadScopes readScopes;

    @BeforeEach
    void setUp(final MongoCollections mongoCollections, final MongoDBTestService mongodb) {
        this.mongoCollections = mongoCollections;
        this.mongodb = mongodb;
        final var catalog = new DbEntitiesCatalog(List.of(
                new DbEntityCatalogEntry("assets", "name", Object.class, "assets:read", List.of()),
                new DbEntityCatalogEntry("users", "full_name", Object.class, DbEntity.ALL_ALLOWED, List.of()),
                new DbEntityCatalogEntry("security_events", "event_id", Object.class, DbEntity.NOBODY_ALLOWED,
                        List.of())));
        readScopes = new CatalogReadScopes(mongoCollections.connection(), new EntityPermissionsUtils(catalog));
        when(subject.isPermitted(anyString()))
                .thenAnswer(invocation -> permissions.contains(invocation.<String>getArgument(0)));
    }

    @Test
    void readsOnlyTheEntitiesTheUserIsPermittedFor() {
        permissions.addAll(List.of("assets:read:" + WEB_01, "assets:read:" + LAPTOP_07));

        final var scope = readScopes.readScope("assets", subject);

        assertThat(readableNames("assets", scope)).containsExactlyInAnyOrder("web-01", "laptop-07");
    }

    @Test
    void readsNothingWithoutPermissions() {
        final var scope = readScopes.readScope("assets", subject);

        assertThat(readableNames("assets", scope)).isEmpty();
    }

    @Test
    void readsTheWholeCollectionWithAWildcardPermission() {
        permissions.add("assets:read:*");

        assertThat(readScopes.readScope("assets", subject)).isEqualTo(ReadScope.unrestricted());
    }

    @Test
    void readsEverythingAsAdmin() {
        when(subject.isPermitted(any(Permission.class))).thenReturn(true);

        assertThat(readScopes.readScope("assets", subject)).isEqualTo(ReadScope.unrestricted());
        assertThat(readScopes.readScope("security_events", subject)).isEqualTo(ReadScope.unrestricted());
    }

    @Test
    void readsCollectionsOpenToEveryone() {
        assertThat(readScopes.readScope("users", subject)).isEqualTo(ReadScope.unrestricted());
    }

    @Test
    void readsNothingOfCollectionsClosedToEveryone() {
        permissions.add("security_events:read:*");

        final var scope = readScopes.readScope("security_events", subject);

        assertThat(mongodb.mongoCollection("security_events").countDocuments(scope.filter())).isZero();
    }

    @Test
    void rejectsCollectionsMissingFromTheCatalog() {
        assertThatThrownBy(() -> readScopes.readScope("streams", subject))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Collection <streams> isn't registered");
    }

    @Test
    void slicesOnlyTheEntitiesTheUserMayRead() {
        permissions.addAll(List.of("assets:read:" + WEB_01, "assets:read:" + DB_01));
        final var attributes = List.of(
                EntityAttribute.builder().id("name").title("Name").searchable(true).build(),
                EntityAttribute.builder().id("category").title("Category").sliceable(true).build());
        final var assets = new SliceableEntity("assets", attributes, new DbQueryCreator("name", attributes));
        final var slicer = new EntitySlicer(mongoCollections,
                (request, titlePermissions) -> EntitiesTitleResponse.EMPTY_RESPONSE);

        final var slices = slicer.slices(assets, SliceRequest.of("category", List.of(), ""),
                readScopes.readScope("assets", subject), (readPermission, id) -> true);

        // laptop-07 (workstation) and printer-02 (no category) aren't readable
        assertThat(slices.slices())
                .containsExactly(new Slice("server", "server", 2), new Slice("database", "database", 1));
    }

    private List<String> readableNames(final String collection, final ReadScope scope) {
        return mongodb.mongoCollection(collection).find(scope.filter())
                .map(document -> document.getString("name"))
                .into(new ArrayList<>());
    }
}
