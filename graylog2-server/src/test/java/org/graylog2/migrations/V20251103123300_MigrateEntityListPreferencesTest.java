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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.assertj.core.api.AbstractStringAssert;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.V20251103123300_MigrateEntityListPreferences.MigrationCompleted;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class V20251103123300_MigrateEntityListPreferencesTest {
    private ClusterConfigService clusterConfigService;
    private Migration migration;
    private MongoConnection connection;
    private MongoCollection<Document> collection;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp(MongoDBTestService mongodb, MongoJackObjectMapperProvider objectMapperProvider) {
        this.clusterConfigService = mock(ClusterConfigService.class);
        this.connection = spy(mongodb.mongoConnection());
        this.migration = new V20251103123300_MigrateEntityListPreferences(clusterConfigService, mongodb.mongoConnection());
        this.collection = mongodb.mongoConnection().getMongoDatabase().getCollection(V20251103123300_MigrateEntityListPreferences.COLLECTION_NAME);
        this.objectMapper = objectMapperProvider.get();
    }

    @Test
    void migrationIsNotRunningTwice() {
        when(clusterConfigService.get(MigrationCompleted.class))
                .thenReturn(new MigrationCompleted(10));

        migration.upgrade();

        verify(connection, never()).getMongoDatabase();
    }

    @Test
    @MongoDBFixtures("V20251103123300_MigrateEntityListPreferences_before.json")
    void migratesExistingPreferences() throws Exception {
        migration.upgrade();

        assertThat(collection.countDocuments()).isEqualTo(16);
        assertSuccessfulMigration(8);
        assertPreference("local:admin", "streams").isEqualTo("""
                {"_id":{"user_id":"local:admin","entity_list_id":"streams"}}""");
        assertPreference("local:admin", "dashboards").isEqualTo("""
                {"_id":{"user_id":"local:admin","entity_list_id":"dashboards"},"sort":{"field":"last_updated_at","order":"asc"},"attributes":{"description":{"status":"show"},"favorite":{"status":"show"},"last_updated_at":{"status":"show"},"owner":{"status":"show"},"_entity_source.source":{"status":"show"},"summary":{"status":"show"},"title":{"status":"show"}}}""");
        assertPreference("local:admin", "saved-searches").isEqualTo("""
                {"_id":{"user_id":"local:admin","entity_list_id":"saved-searches"},"sort":{"field":"last_updated_at","order":"desc"},"attributes":{"created_at":{"status":"show"},"favorite":{"status":"show"},"last_updated_at":{"status":"show"},"title":{"status":"show"}}}""");
        assertPreference("local:admin", "index-set-field-types").isEqualTo("""
                {"_id":{"user_id":"local:admin","entity_list_id":"index-set-field-types"},"per_page":20,"sort":{"field":"field_name","order":"asc"}}""");
        assertPreference("5ffdbca8eeae7f4edf068822", "dashboards").isEqualTo("""
                {"_id":{"user_id":"5ffdbca8eeae7f4edf068822","entity_list_id":"dashboards"},"per_page":20,"attributes":{"description":{"status":"show"},"favorite":{"status":"show"},"owner":{"status":"show"},"summary":{"status":"show"},"title":{"status":"show"}}}""");
        assertPreference("646f403132107352a00146c1", "streams").isEqualTo("""
                {"_id":{"user_id":"646f403132107352a00146c1","entity_list_id":"streams"}}""");
        assertPreference("646f403132107352a00146c1", "dashboards").isEqualTo("""
                {"_id":{"user_id":"646f403132107352a00146c1","entity_list_id":"dashboards"}}""");
        assertPreference("local:admin", "reports_history").isEqualTo("""
                {"_id":{"user_id":"local:admin","entity_list_id":"reports_history"},"per_page":10,"sort":{"field":"generated_at","order":"desc"},"attributes":{"generated_at":{"status":"show"},"has_asset":{"status":"show"},"status":{"status":"show"},"message":{"status":"show"}}}""");
        assertPreference("local:admin", "reports").isEqualTo("""
                {"_id":{"user_id":"local:admin","entity_list_id":"reports"},"sort":{"field":"title","order":"asc"}}""");
        assertPreference("local:admin", "events").isEqualTo("""
                {"_id":{"user_id":"local:admin","entity_list_id":"events"},"per_page":100,"sort":{"field":"timestamp","order":"desc"},"custom_preferences":{"showMetrics":true},"attributes":{"message":{"status":"show"},"event_definition_id":{"status":"show"},"priority":{"status":"show"},"timestamp":{"status":"show"},"alert":{"status":"show"}}}""");
        assertPreference("local:admin", "security_events").isEqualTo("""
                {"_id":{"user_id":"local:admin","entity_list_id":"security_events"},"per_page":100,"sort":{"field":"timestamp","order":"desc"},"custom_preferences":{"showMetrics":true},"attributes":{"message":{"status":"show"},"event_definition_id":{"status":"show"},"owner":{"status":"show"},"priority":{"status":"show"},"scores":{"status":"show"},"status":{"status":"show"},"timestamp":{"status":"show"},"alert":{"status":"show"}}}""");
        assertPreference("local:admin", "event_definitions").isEqualTo("""
                {"_id":{"user_id":"local:admin","entity_list_id":"event_definitions"},"per_page":100,"attributes":{"description":{"status":"show"},"matched_at":{"status":"show"},"priority":{"status":"show"},"scheduling":{"status":"show"},"_entity_source.source":{"status":"show"},"status":{"status":"show"},"title":{"status":"show"}}}""");
        assertPreference("5ffdbca8eeae7f4edf068822", "streams").isEqualTo("""
                {"_id":{"user_id":"5ffdbca8eeae7f4edf068822","entity_list_id":"streams"},"per_page":50,"sort":{"field":"title","order":"asc"}}""");
        assertPreference("local:admin", "token_usage").isEqualTo("""
                {"_id":{"user_id":"local:admin","entity_list_id":"token_usage"},"sort":{"field":"last_access","order":"desc"}}""");
        assertPreference("5ffdbca8eeae7f4edf068822", "saved-searches").isEqualTo("""
                {"_id":{"user_id":"5ffdbca8eeae7f4edf068822","entity_list_id":"saved-searches"},"attributes":{"created_at":{"status":"show"},"description":{"status":"show"},"favorite":{"status":"show"},"owner":{"status":"show"},"summary":{"status":"show"},"title":{"status":"show"}}}""");
        assertPreference("5ffdbca8eeae7f4edf068822", "saved-searches-for-investigations").isEqualTo("""
                {"_id":{"user_id":"5ffdbca8eeae7f4edf068822","entity_list_id":"saved-searches-for-investigations"},"per_page":20}""");
    }

    @Test
    void writesMigrationComplete() {
        migration.upgrade();

        assertSuccessfulMigration(0);
    }

    private MigrationCompleted captureMigrationCompleted() {
        final ArgumentCaptor<MigrationCompleted> migrationCompletedCaptor = ArgumentCaptor.forClass(MigrationCompleted.class);
        verify(clusterConfigService, times(1)).write(migrationCompletedCaptor.capture());
        return migrationCompletedCaptor.getValue();
    }

    private void assertSuccessfulMigration(long count) {
        final var migrationCompleted = captureMigrationCompleted();

        assertThat(migrationCompleted).isNotNull()
                .satisfies(completed -> {
                    assertThat(completed.updatedTables()).isEqualTo(count);
                });
    }

    private AbstractStringAssert<?> assertPreference(String user, String table) throws JsonProcessingException {
        return assertThat(getPreference(user, table));
    }

    private String getPreference(String user, String table) throws JsonProcessingException {
        final var document = collection.find(Filters.and(Filters.eq("_id.user_id", user), Filters.eq("_id.entity_list_id", table))).first();
        assertThat(document).isNotNull();
        return objectMapper.writeValueAsString(document);
    }
}
