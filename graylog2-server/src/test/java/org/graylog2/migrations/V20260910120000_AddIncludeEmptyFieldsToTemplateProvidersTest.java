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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class V20260910120000_AddIncludeEmptyFieldsToTemplateProvidersTest {

    private final NodeId nodeId = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);

    private ClusterConfigServiceImpl clusterConfigService;
    private Migration migration;
    private MongoCollection<Document> collection;
    private MongoConnection connection;

    @BeforeEach
    public void setUp(MongoCollections mongoCollections) {
        this.connection = mongoCollections.connection();
        this.clusterConfigService = new ClusterConfigServiceImpl(objectMapperProvider,
                connection,
                nodeId,
                new RestrictedChainingClassLoader(
                        new ChainingClassLoader(getClass().getClassLoader()), SafeClasses.allGraylogInternal()),
                new ClusterEventBus());

        this.collection = connection.getMongoDatabase().getCollection("event_definitions");
        this.migration = new V20260910120000_AddIncludeEmptyFieldsToTemplateProviders(connection, clusterConfigService);
    }

    private Document providerOf(String title, String fieldName) {
        return providerOf(title, fieldName, 0);
    }

    private Document providerOf(String title, String fieldName, int index) {
        final Document definition = collection.find(Filters.eq("title", title)).first();
        assertThat(definition).isNotNull();

        return definition.get("field_spec", Document.class)
                .get(fieldName, Document.class)
                .getList("providers", Document.class)
                .get(index);
    }

    @Test
    public void createdAt() {
        assertThat(migration.createdAt()).isEqualTo(ZonedDateTime.parse("2026-09-10T12:00:00Z"));
    }

    @Test
    @MongoDBFixtures("V20260910120000_AddIncludeEmptyFieldsToTemplateProvidersTest.json")
    public void upgradeStampsExistingTemplateProviders() {
        final long totalBefore = collection.countDocuments();

        migration.upgrade();

        assertThat(collection.countDocuments())
                .withFailMessage("No event definitions should be deleted by the migration!")
                .isEqualTo(totalBefore);

        // A plain template provider keeps today's behavior: empty values are written to the event.
        assertThat(providerOf("Plain template field", "user").getBoolean("include_empty_fields")).isTrue();

        // require_values already turns empty values into errors, so the option must be off.
        assertThat(providerOf("Required template field", "host").getBoolean("include_empty_fields")).isFalse();

        // An explicit value set by a newer UI must not be overwritten.
        assertThat(providerOf("Already stamped", "already").getBoolean("include_empty_fields")).isFalse();

        // Non-template providers are untouched.
        assertThat(providerOf("Lookup provider only", "looked_up")).doesNotContainKey("include_empty_fields");

        // A definition without a field_spec must survive untouched.
        assertThat(collection.find(Filters.eq("title", "No field spec at all")).first()).isNotNull();

        // Both providers under one field name are stamped, and so is a second field.
        assertThat(providerOf("Multiple template fields", "first", 0).getBoolean("include_empty_fields")).isTrue();
        assertThat(providerOf("Multiple template fields", "first", 1).getBoolean("include_empty_fields")).isTrue();
        assertThat(providerOf("Multiple template fields", "second", 0).getBoolean("include_empty_fields")).isFalse();

        // A malformed definition is skipped rather than aborting the migration.
        assertThat(providerOf("Malformed require values", "broken", 0)).doesNotContainKey("include_empty_fields");

        final V20260910120000_AddIncludeEmptyFieldsToTemplateProviders.MigrationCompleted completed =
                clusterConfigService.get(V20260910120000_AddIncludeEmptyFieldsToTemplateProviders.MigrationCompleted.class);
        assertThat(completed).isNotNull();
        assertThat(completed.modifiedEventDefinitions()).isEqualTo(3L);
        assertThat(completed.skippedEventDefinitions()).isEqualTo(1L);
    }

    @Test
    @MongoDBFixtures("V20260910120000_AddIncludeEmptyFieldsToTemplateProvidersTest.json")
    public void malformedDefinitionDoesNotAbortTheMigration() {
        // A backfill must never block server startup, so a bad document is skipped, not fatal.
        assertThatCode(() -> migration.upgrade()).doesNotThrowAnyException();

        // Definitions after the malformed one in iteration order are still stamped.
        assertThat(providerOf("Plain template field", "user", 0).getBoolean("include_empty_fields")).isTrue();
    }

    @Test
    @MongoDBFixtures("V20260910120000_AddIncludeEmptyFieldsToTemplateProvidersTest.json")
    public void backfillIsIdempotent() {
        migration.upgrade();
        final List<Document> afterFirstRun = collection.find().into(new ArrayList<>());

        // Clear the completion marker so the second run actually walks the documents again.
        connection.getMongoDatabase().getCollection("cluster_config")
                .deleteMany(Filters.eq("type",
                        V20260910120000_AddIncludeEmptyFieldsToTemplateProviders.MigrationCompleted.class.getCanonicalName()));

        migration.upgrade();

        assertThat(collection.find().into(new ArrayList<>())).isEqualTo(afterFirstRun);
    }

    @Test
    @MongoDBFixtures("V20260910120000_AddIncludeEmptyFieldsToTemplateProvidersTest.json")
    public void upgradeSkipsWorkWhenAlreadyCompleted() {
        migration.upgrade();
        final List<Document> afterFirstRun = collection.find().into(new ArrayList<>());

        migration.upgrade();

        assertThat(collection.find().into(new ArrayList<>())).isEqualTo(afterFirstRun);
    }
}
