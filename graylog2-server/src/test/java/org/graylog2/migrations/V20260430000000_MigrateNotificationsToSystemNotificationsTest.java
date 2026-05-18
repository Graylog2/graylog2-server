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

import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.graylog.events.processor.systemnotification.SystemNotificationRenderService;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.database.MongoCollections;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.SystemNotificationDto;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
class V20260430000000_MigrateNotificationsToSystemNotificationsTest {

    @Mock
    private ClusterConfigService clusterConfigService;

    @Mock
    private SystemNotificationRenderService renderService;

    private MongoCollections mongoCollections;
    private V20260430000000_MigrateNotificationsToSystemNotifications migration;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        this.mongoCollections = mongoCollections;
        this.migration = new V20260430000000_MigrateNotificationsToSystemNotifications(
                clusterConfigService, mongoCollections, renderService);
    }

    @Test
    @MongoDBFixtures("V20260430000000_MigrateNotificationsToSystemNotificationsTest.json")
    void migratesAllNotifications() {
        when(renderService.render(any(Notification.class), any(), isNull()))
                .thenReturn(new SystemNotificationRenderService.RenderResponse("Rendered Title", "Rendered Description"));

        migration.upgrade();

        final var newCollection = mongoCollections.collection("system_notifications", SystemNotificationDto.class);
        assertThat(newCollection.countDocuments()).isEqualTo(3);

        // Verify first document (no key, high priority)
        final var esRed = newCollection.find(Filters.eq("type", "es_cluster_red")).first();
        assertThat(esRed).isNotNull();
        assertThat(esRed.key()).isNull();
        assertThat(esRed.priority()).isEqualTo("high");
        assertThat(esRed.nodeId()).isEqualTo("node-1");
        assertThat(esRed.title()).isEqualTo("Rendered Title");
        assertThat(esRed.description()).isEqualTo("Rendered Description");
        assertThat(esRed.isRead()).isFalse();
        assertThat(esRed.actor()).isNull();
        assertThat(esRed.lastChanged()).isNull();
        assertThat(esRed.details()).containsEntry("reason", "shards unassigned");

        // Verify keyed document
        final var inputFailing = newCollection.find(Filters.eq("type", "input_failing")).first();
        assertThat(inputFailing).isNotNull();
        assertThat(inputFailing.key()).isEqualTo("syslog-udp-01");
        assertThat(inputFailing.priority()).isEqualTo("normal");
        assertThat(inputFailing.nodeId()).isEqualTo("node-2");

        // Verify document with missing priority defaults to "normal"
        final var outdated = newCollection.find(Filters.eq("type", "outdated_version")).first();
        assertThat(outdated).isNotNull();
        assertThat(outdated.priority()).isEqualTo("normal");

        // Old collection should be dropped
        final Set<String> collections = mongoCollections.mongoConnection().getMongoDatabase()
                .listCollectionNames().into(new HashSet<>());
        assertThat(collections).doesNotContain("notifications");

        // MigrationCompleted marker should be written
        verify(clusterConfigService).write(any(V20260430000000_MigrateNotificationsToSystemNotifications.MigrationCompleted.class));
    }

    @Test
    @MongoDBFixtures("V20260430000000_MigrateNotificationsToSystemNotificationsTest.json")
    void skipsWhenAlreadyCompleted() {
        when(clusterConfigService.get(V20260430000000_MigrateNotificationsToSystemNotifications.MigrationCompleted.class))
                .thenReturn(new V20260430000000_MigrateNotificationsToSystemNotifications.MigrationCompleted());

        migration.upgrade();

        // Render service should never be called
        verify(renderService, never()).render(any(), any(), any());

        // New collection should be empty (nothing migrated)
        final var newCollection = mongoCollections.collection("system_notifications", SystemNotificationDto.class);
        assertThat(newCollection.countDocuments()).isEqualTo(0);

        // Old collection should still exist
        final Set<String> collections = mongoCollections.mongoConnection().getMongoDatabase()
                .listCollectionNames().into(new HashSet<>());
        assertThat(collections).contains("notifications");
    }

    @Test
    @MongoDBFixtures("V20260430000000_MigrateNotificationsToSystemNotificationsTest.json")
    void continuesWhenRenderingFails() {
        when(renderService.render(any(Notification.class), any(), isNull()))
                .thenThrow(new RuntimeException("Template not found"));

        migration.upgrade();

        // All 3 documents should still be migrated, just with null title/description
        final var newCollection = mongoCollections.collection("system_notifications", SystemNotificationDto.class);
        assertThat(newCollection.countDocuments()).isEqualTo(3);

        final var esRed = newCollection.find(Filters.eq("type", "es_cluster_red")).first();
        assertThat(esRed).isNotNull();
        assertThat(esRed.title()).isNull();
        assertThat(esRed.description()).isNull();
        // Other fields should still be populated correctly
        assertThat(esRed.priority()).isEqualTo("high");
        assertThat(esRed.details()).containsEntry("reason", "shards unassigned");

        verify(clusterConfigService).write(any(V20260430000000_MigrateNotificationsToSystemNotifications.MigrationCompleted.class));
    }

    @Test
    void handlesEmptyCollection() {
        migration.upgrade();

        final var newCollection = mongoCollections.collection("system_notifications", SystemNotificationDto.class);
        assertThat(newCollection.countDocuments()).isEqualTo(0);

        verify(clusterConfigService).write(any(V20260430000000_MigrateNotificationsToSystemNotifications.MigrationCompleted.class));
    }
}
