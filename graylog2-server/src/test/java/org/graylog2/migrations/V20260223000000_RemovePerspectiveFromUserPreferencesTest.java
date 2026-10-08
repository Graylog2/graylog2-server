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
class V20260223000000_RemovePerspectiveFromUserPreferencesTest {
    @Mock
    private ClusterConfigService clusterConfigService;

    private Migration migration;
    private MongoCollection<Document> usersCollection;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        this.migration = new V20260223000000_RemovePerspectiveFromUserPreferences(clusterConfigService, mongodb.mongoConnection());
        this.usersCollection = mongodb.mongoConnection().getMongoDatabase().getCollection("users");
    }

    @Test
    void alreadyMigrated() {
        when(clusterConfigService.get(V20260223000000_RemovePerspectiveFromUserPreferences.MigrationCompleted.class))
                .thenReturn(new V20260223000000_RemovePerspectiveFromUserPreferences.MigrationCompleted(2));

        this.migration.upgrade();

        verify(clusterConfigService, never()).write(any());
    }

    @Test
    @MongoDBFixtures("V20260223000000_RemovePerspectiveFromUserPreferences/users_without_perspective.json")
    void doesNotModifyUsersWithoutPerspective() {
        this.migration.upgrade();

        var gary = usersCollection.find(Filters.eq("username", "gary")).first();
        assertThat(gary.getEmbedded(List.of("preferences", "perspective"), String.class)).isNull();
        assertThat(gary.getEmbedded(List.of("preferences", "updateUnfocussed"), Boolean.class)).isFalse();
        assertThat(gary.getEmbedded(List.of("preferences", "enableSmartSearch"), Boolean.class)).isTrue();

        assertThat(migrationMarkerIsWritten().modified()).isZero();
    }

    @Test
    @MongoDBFixtures("V20260223000000_RemovePerspectiveFromUserPreferences/users_with_perspective.json")
    void removesPerspectiveFromUsersWithExistingPreferences() {
        this.migration.upgrade();

        var gary = usersCollection.find(Filters.eq("username", "gary")).first();
        assertThat(gary.getEmbedded(List.of("preferences", "perspective"), String.class)).isNull();
        assertThat(gary.getEmbedded(List.of("preferences", "updateUnfocussed"), Boolean.class)).isFalse();
        assertThat(gary.getEmbedded(List.of("preferences", "enableSmartSearch"), Boolean.class)).isTrue();

        var larry = usersCollection.find(Filters.eq("username", "larry")).first();
        assertThat(larry.getEmbedded(List.of("preferences", "perspective"), String.class)).isNull();
        assertThat(larry.getEmbedded(List.of("preferences", "updateUnfocussed"), Boolean.class)).isFalse();
        assertThat(larry.getEmbedded(List.of("preferences", "enableSmartSearch"), Boolean.class)).isTrue();

        assertThat(migrationMarkerIsWritten().modified()).isEqualTo(2L);
    }

    private V20260223000000_RemovePerspectiveFromUserPreferences.MigrationCompleted migrationMarkerIsWritten() {
        var migrationCompleted = ArgumentCaptor.forClass(V20260223000000_RemovePerspectiveFromUserPreferences.MigrationCompleted.class);
        verify(clusterConfigService, times(1)).write(migrationCompleted.capture());
        return migrationCompleted.getValue();
    }
}
