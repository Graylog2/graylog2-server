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
public class V20260416120000_RemoveStrayFieldsFromInputsTest {

    private final NodeId nodeId = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);

    private ClusterConfigServiceImpl clusterConfigService;
    private Migration migration;
    private MongoCollection<Document> collection;

    @BeforeEach
    public void setUp(MongoCollections mongoCollections) {
        final MongoConnection connection = mongoCollections.connection();
        this.clusterConfigService = new ClusterConfigServiceImpl(objectMapperProvider,
                connection,
                nodeId,
                new RestrictedChainingClassLoader(
                        new ChainingClassLoader(getClass().getClassLoader()), SafeClasses.allGraylogInternal()),
                new ClusterEventBus());

        this.collection = connection.getMongoDatabase().getCollection("inputs");
        this.migration = new V20260416120000_RemoveStrayFieldsFromInputs(connection, clusterConfigService);
    }

    @Test
    public void createdAt() {
        assertThat(migration.createdAt()).isEqualTo(ZonedDateTime.parse("2026-04-16T12:00:00Z"));
    }

    @Test
    @MongoDBFixtures("V20260416120000_RemoveStrayFieldsFromInputsTest.json")
    public void upgradeRemovesStrayFieldsAndPreservesDocuments() {
        final long totalBefore = collection.countDocuments();

        migration.upgrade();

        // No documents should be deleted
        assertThat(collection.countDocuments())
                .withFailMessage("No input documents should be deleted by the migration!")
                .isEqualTo(totalBefore);

        // The stray "fields" key should be gone from all documents
        assertThat(collection.countDocuments(Filters.exists("fields")))
                .withFailMessage("The migration should have removed the stray \"fields\" key from all input documents!")
                .isEqualTo(0L);

        // Legitimate fields should be untouched
        final Document syslogInput = collection.find(Filters.eq("title", "Syslog UDP")).first();
        assertThat(syslogInput).isNotNull();
        assertThat(syslogInput.getString("type")).isEqualTo("org.graylog2.inputs.syslog.udp.SyslogUDPInput");
        assertThat(syslogInput.get("configuration")).isNotNull();

        // The clean input (no stray fields) should be completely unchanged
        final Document beatsInput = collection.find(Filters.eq("title", "Beats")).first();
        assertThat(beatsInput).isNotNull();
        assertThat(beatsInput.getString("type")).isEqualTo("org.graylog.plugins.beats.BeatsInput");

        // Migration completion should be recorded
        final V20260416120000_RemoveStrayFieldsFromInputs.MigrationCompleted completed =
                clusterConfigService.get(V20260416120000_RemoveStrayFieldsFromInputs.MigrationCompleted.class);
        assertThat(completed).isNotNull();
        assertThat(completed.modifiedInputs()).isEqualTo(2L);
    }

    @Test
    @MongoDBFixtures("V20260416120000_RemoveStrayFieldsFromInputsTest.json")
    public void upgradeIsIdempotent() {
        migration.upgrade();

        // Run again — should be a no-op
        migration.upgrade();

        // Still only 2 modified from the first run
        final V20260416120000_RemoveStrayFieldsFromInputs.MigrationCompleted completed =
                clusterConfigService.get(V20260416120000_RemoveStrayFieldsFromInputs.MigrationCompleted.class);
        assertThat(completed).isNotNull();
        assertThat(completed.modifiedInputs()).isEqualTo(2L);
    }

    @Test
    public void upgradeWithNoAffectedDocuments() {
        // Empty collection — migration should succeed gracefully
        migration.upgrade();

        final V20260416120000_RemoveStrayFieldsFromInputs.MigrationCompleted completed =
                clusterConfigService.get(V20260416120000_RemoveStrayFieldsFromInputs.MigrationCompleted.class);
        assertThat(completed).isNotNull();
        assertThat(completed.modifiedInputs()).isEqualTo(0L);
    }
}
