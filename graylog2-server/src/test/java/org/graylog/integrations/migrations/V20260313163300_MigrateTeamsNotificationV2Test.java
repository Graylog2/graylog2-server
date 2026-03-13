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
 * <http://www.mongodb.org/licensing/server-side-public-license>.
 */
package org.graylog.integrations.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.database.MongoCollections;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class V20260313163300_MigrateTeamsNotificationV2Test {

    private V20260313163300_MigrateTeamsNotificationV2 migration;

    @Mock
    private ClusterConfigService clusterConfigService;

    private MongoCollection<Document> eventNotifications;

    @BeforeEach
    public void setUp(MongoCollections mongoCollections) {
        migration = new V20260313163300_MigrateTeamsNotificationV2(
                mongoCollections.connection(), clusterConfigService);
        this.eventNotifications = mongoCollections.connection()
                .getMongoDatabase()
                .getCollection("event_notifications");
    }

    @Test
    @MongoDBFixtures("V20260313163300_MigrateTeamsNotificationV2Test.json")
    public void testSkipsWhenAlreadyCompleted() {
        when(clusterConfigService.get(V20260313163300_MigrateTeamsNotificationV2.MigrationCompletion.class))
                .thenReturn(V20260313163300_MigrateTeamsNotificationV2.MigrationCompletion.create());

        long countBefore = eventNotifications.countDocuments();

        migration.upgrade();

        // No documents modified
        assertThat(eventNotifications.countDocuments()).isEqualTo(countBefore);
        verify(clusterConfigService, never()).write(any());
    }

    @Test
    @MongoDBFixtures("V20260313163300_MigrateTeamsNotificationV2Test.json")
    public void testSetsTimeZoneToUTC() {
        when(clusterConfigService.get(V20260313163300_MigrateTeamsNotificationV2.MigrationCompletion.class))
                .thenReturn(null);

        // Before migration: time_zone is "Europe/Berlin"
        var notificationBefore = eventNotifications.find(
                Filters.eq("_id", new ObjectId("69b409a722ad2cd99b88a69a"))).first();
        assertThat(notificationBefore.get("config", Document.class).getString("time_zone"))
                .isEqualTo("Europe/Berlin");

        migration.upgrade();

        // After migration: time_zone is "UTC"
        var notificationAfter = eventNotifications.find(
                Filters.eq("_id", new ObjectId("69b409a722ad2cd99b88a69a"))).first();
        assertThat(notificationAfter.get("config", Document.class).getString("time_zone"))
                .isEqualTo("UTC");
    }

    @Test
    @MongoDBFixtures("V20260313163300_MigrateTeamsNotificationV2Test.json")
    public void testUpdatesAdaptiveCardTimestamp() {
        when(clusterConfigService.get(V20260313163300_MigrateTeamsNotificationV2.MigrationCompletion.class))
                .thenReturn(null);

        // Before migration: raw timestamp placeholder, no DATE()/TIME() functions
        var notificationBefore = eventNotifications.find(
                Filters.eq("_id", new ObjectId("69b409a722ad2cd99b88a69a"))).first();
        String cardBefore = notificationBefore.get("config", Document.class).getString("adaptive_card");
        assertThat(cardBefore).contains("\"value\": \"${event.timestamp_processing}\"");
        assertThat(cardBefore).doesNotContain("DATE(");
        assertThat(cardBefore).doesNotContain("TIME(");

        migration.upgrade();

        // After migration: DATE()/TIME() functions present, raw placeholder gone
        var notificationAfter = eventNotifications.find(
                Filters.eq("_id", new ObjectId("69b409a722ad2cd99b88a69a"))).first();
        String cardAfter = notificationAfter.get("config", Document.class).getString("adaptive_card");
        assertThat(cardAfter).contains("{{DATE(${event.timestamp_processing},SHORT)}} at {{TIME(${event.timestamp_processing})}}");
        assertThat(cardAfter).doesNotContain("\"value\": \"${event.timestamp_processing}\"");
    }

    @Test
    @MongoDBFixtures("V20260313163300_MigrateTeamsNotificationV2Test.json")
    public void testSkipsNonTeamsV2Notifications() {
        when(clusterConfigService.get(V20260313163300_MigrateTeamsNotificationV2.MigrationCompletion.class))
                .thenReturn(null);

        // Before migration: non-teams-v2 doc has no time_zone
        var otherBefore = eventNotifications.find(
                Filters.eq("_id", new ObjectId("69b409a722ad2cd99b88a69b"))).first();
        assertThat(otherBefore.get("config", Document.class).getString("time_zone")).isNull();

        migration.upgrade();

        // After migration: non-teams-v2 doc is unchanged
        var otherAfter = eventNotifications.find(
                Filters.eq("_id", new ObjectId("69b409a722ad2cd99b88a69b"))).first();
        assertThat(otherAfter.get("config", Document.class).getString("time_zone")).isNull();
    }

    @Test
    @MongoDBFixtures("V20260313163300_MigrateTeamsNotificationV2Test.json")
    public void testWritesCompletionMarker() {
        when(clusterConfigService.get(V20260313163300_MigrateTeamsNotificationV2.MigrationCompletion.class))
                .thenReturn(null);

        migration.upgrade();

        verify(clusterConfigService).write(
                any(V20260313163300_MigrateTeamsNotificationV2.MigrationCompletion.class));
    }
}
