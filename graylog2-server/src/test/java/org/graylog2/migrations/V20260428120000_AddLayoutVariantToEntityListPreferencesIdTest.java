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
package org.graylog2.migrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.security.RestrictedChainingClassLoader;
import org.graylog2.security.SafeClasses;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class V20260428120000_AddLayoutVariantToEntityListPreferencesIdTest {

    private final NodeId nodeId = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);

    private ClusterConfigServiceImpl clusterConfigService;
    private Migration migration;
    private MongoCollection<Document> collection;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        final MongoConnection connection = mongoCollections.connection();
        this.clusterConfigService = new ClusterConfigServiceImpl(
                objectMapperProvider,
                connection,
                nodeId,
                new RestrictedChainingClassLoader(
                        new ChainingClassLoader(getClass().getClassLoader()), SafeClasses.allGraylogInternal()),
                new ClusterEventBus());
        this.collection = connection.getMongoDatabase()
                .getCollection(V20260428120000_AddLayoutVariantToEntityListPreferencesId.COLLECTION_NAME);
        this.migration = new V20260428120000_AddLayoutVariantToEntityListPreferencesId(clusterConfigService, connection);
    }

    @Test
    void createdAt() {
        assertThat(migration.createdAt()).isEqualTo(ZonedDateTime.parse("2026-04-28T12:00:00Z"));
    }

    @Test
    @MongoDBFixtures("V20260428120000_AddLayoutVariantToEntityListPreferencesIdTest.json")
    void upgradeAddsMissingLayoutVariant() {
        migration.upgrade();

        assertThat(collection.countDocuments()).isEqualTo(4L);
        assertThat(collection.countDocuments(Filters.exists("_id.layout_variant", false))).isEqualTo(0L);
        assertThat(collection.countDocuments(Filters.eq("_id.layout_variant", "#general#"))).isEqualTo(4L);

        var completed = clusterConfigService.get(
                V20260428120000_AddLayoutVariantToEntityListPreferencesId.MigrationCompleted.class);
        assertThat(completed).isNotNull();
        assertThat(completed.migratedDocuments()).isEqualTo(3L);
    }

    @Test
    @MongoDBFixtures("V20260428120000_AddLayoutVariantToEntityListPreferencesIdTest.json")
    void upgradePreservesOtherFields() {
        migration.upgrade();

        final Document streams = collection.find(Filters.and(
                Filters.eq("_id.user_id", "local:admin"),
                Filters.eq("_id.entity_list_id", "streams")
        )).first();
        assertThat(streams).isNotNull();
        assertThat(streams.getInteger("per_page")).isEqualTo(20);
        assertThat(streams.get("sort", Document.class).getString("field")).isEqualTo("title");

        final Document dashboards = collection.find(Filters.and(
                Filters.eq("_id.user_id", "local:admin"),
                Filters.eq("_id.entity_list_id", "dashboards")
        )).first();
        assertThat(dashboards).isNotNull();
        assertThat(dashboards.get("attributes", Document.class)).isNotNull();
    }

    @Test
    @MongoDBFixtures("V20260428120000_AddLayoutVariantToEntityListPreferencesIdTest.json")
    void upgradeDoesNotModifyAlreadyMigratedDocuments() {
        migration.upgrade();

        final Document events = collection.find(Filters.and(
                Filters.eq("_id.user_id", "local:admin"),
                Filters.eq("_id.entity_list_id", "events")
        )).first();
        assertThat(events).isNotNull();
        assertThat(events.get("_id", Document.class).getString("layout_variant")).isEqualTo("#general#");
        assertThat(events.getInteger("per_page")).isEqualTo(100);
    }

    @Test
    void upgradeWithEmptyCollection() {
        migration.upgrade();

        assertThat(collection.countDocuments()).isEqualTo(0L);
        final var completed = clusterConfigService.get(
                V20260428120000_AddLayoutVariantToEntityListPreferencesId.MigrationCompleted.class);
        assertThat(completed).isNotNull();
        assertThat(completed.migratedDocuments()).isEqualTo(0L);
    }
}
