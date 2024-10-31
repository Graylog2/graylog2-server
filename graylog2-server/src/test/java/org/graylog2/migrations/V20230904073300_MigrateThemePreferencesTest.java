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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
class V20230904073300_MigrateThemePreferencesTest {
    @Mock
    private ClusterConfigService clusterConfigService;

    private Migration migration;
    private MongoCollection<Document> usersCollection;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        this.migration = new V20230904073300_MigrateThemePreferences(clusterConfigService, mongodb.mongoConnection());
        this.usersCollection = mongodb.mongoConnection().getMongoDatabase().getCollection("users");
    }

    @Test
    void alreadyMigrated() {
        thisMigrationHasRun();

        this.migration.upgrade();

        verify(clusterConfigService, never()).write(any());
    }

    @Test
    @MongoDBFixtures({"V20230904073300_MigrateThemePreferences/no_users_with_theme_preferences.json"})
    void doesNotDoAnythingWhenUsersHaveNoPreferences() {
        this.migration.upgrade();

        var gary = usersCollection.find(Filters.eq("username", "gary")).first();
        assertThat(gary.getEmbedded(List.of("preferences", "themeMode"), String.class)).isNull();

        assertThat(migrationMarkerIsWritten()).satisfies(migrationMarker -> {
            assertThat(migrationMarker.matched()).isZero();
            assertThat(migrationMarker.modified()).isZero();
        });
    }

    @Test
    @MongoDBFixtures({"V20230904073300_MigrateThemePreferences/users_with_theme_preferences.json"})
    void migratesUsersWithExistingPreferences() {
        this.migration.upgrade();

        var gary = usersCollection.find(Filters.eq("username", "gary")).first();
        assertThat(gary.getEmbedded(List.of("preferences", "themeMode"), String.class)).isEqualTo("dark");
        var lightgary = usersCollection.find(Filters.eq("username", "lightgary")).first();
        assertThat(lightgary.getEmbedded(List.of("preferences", "themeMode"), String.class)).isEqualTo("light");

        assertThat(migrationMarkerIsWritten()).satisfies(migrationMarker -> {
            assertThat(migrationMarker.matched()).isEqualTo(2L);
            assertThat(migrationMarker.modified()).isEqualTo(2L);
        });
    }

    private V20230904073300_MigrateThemePreferences.MigrationCompleted migrationMarkerIsWritten() {
        var migrationCompleted = ArgumentCaptor.forClass(V20230904073300_MigrateThemePreferences.MigrationCompleted.class);
        verify(clusterConfigService, times(1)).write(migrationCompleted.capture());

        return migrationCompleted.getValue();
    }

    void thisMigrationHasRun() {
        when(clusterConfigService.get(V20230904073300_MigrateThemePreferences.MigrationCompleted.class)).thenReturn(new V20230904073300_MigrateThemePreferences.MigrationCompleted(42, 23));
    }
}
