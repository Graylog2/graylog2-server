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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.ContentPackInstallationPersistenceService;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ContentPackUninstallation;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.migrations.V20230601104500_AddSourcesPageV2.MigrationCompleted;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
class V20230601104500_AddSourcesPageV2Test {
    private static final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    @Mock
    private ClusterConfigService clusterConfigService;

    @Mock
    private StreamService streamService;

    @Mock
    private NotificationService notificationService;

    private V20230601104500_AddSourcesPageV2 migration;

    static class TestContentPackService extends ContentPackService {
        public TestContentPackService() {
            super(null, null, Map.of(), null, null);
        }

        @Override
        public ContentPackInstallation installContentPack(ContentPack contentPack, Map<String, ValueReference> parameters, String comment, String user) {
            return ContentPackInstallation.builder()
                    .contentPackId(contentPack.id())
                    .createdBy(user)
                    .comment(comment)
                    .contentPackRevision(contentPack.revision())
                    .parameters(ImmutableMap.copyOf(parameters))
                    .createdAt(Instant.now())
                    .entities(ImmutableSet.of())
                    .build();
        }

        @Override
        public ContentPackUninstallation uninstallContentPack(ContentPack contentPack, ContentPackInstallation installation) {
            return ContentPackUninstallation.builder()
                    .entities(ImmutableSet.of())
                    .failedEntities(ImmutableSet.of())
                    .skippedEntities(ImmutableSet.of())
                    .build();
        }
    }

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        var mapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        var mongoConnection = mongodb.mongoConnection();
        ContentPackPersistenceService contentPackPersistenceService = new ContentPackPersistenceService(mapperProvider, mongoConnection, streamService);
        ContentPackInstallationPersistenceService contentPackInstallationPersistenceService = new ContentPackInstallationPersistenceService(mapperProvider, mongoConnection);
        ContentPackService contentPackService = new TestContentPackService();
        this.migration = new V20230601104500_AddSourcesPageV2(contentPackService, objectMapper, clusterConfigService,
                contentPackPersistenceService, contentPackInstallationPersistenceService, mongoConnection, notificationService);
    }

    @Test
    void alreadyMigrated() {
        thisMigrationHasRun();

        this.migration.upgrade();

        verify(clusterConfigService, never()).get(V20191219090834_AddSourcesPage.MigrationCompleted.class);
        verify(clusterConfigService, never()).write(any());
    }

    @Test
    void freshInstallInstallsNewSourcesPage() {
        thisMigrationHasNotRun();

        this.migration.upgrade();

        var migrationCompleted = expectMigrationCompleted();

        assertThat(migrationCompleted.contentPackId()).isNotBlank();
        assertThat(migrationCompleted.installedContentPack()).isTrue();
        assertThat(migrationCompleted.uninstalledPreviousRevision()).isFalse();
    }

    @Test
    @MongoDBFixtures({"V20230601104500_AddSourcesPageV2/previousMigration.json", "V20230601104500_AddSourcesPageV2/previousInstallationWithoutLocalModifications.json"})
    void previousInstallationWithoutLocalModificationsIsUninstalled() {
        previousMigrationHasRun();
        thisMigrationHasNotRun();

        this.migration.upgrade();

        var migrationCompleted = expectMigrationCompleted();

        assertThat(migrationCompleted.contentPackId()).isNotBlank();
        assertThat(migrationCompleted.installedContentPack()).isTrue();
        assertThat(migrationCompleted.uninstalledPreviousRevision()).isTrue();
    }

    @Test
    @MongoDBFixtures({"V20230601104500_AddSourcesPageV2/previousMigration.json", "V20230601104500_AddSourcesPageV2/previousInstallationWithLocalModifications.json"})
    void previousInstallationWithLocalModificationsIsKept() {
        previousMigrationHasRun();
        thisMigrationHasNotRun();
        when(notificationService.buildNow()).thenReturn(new NotificationImpl().addTimestamp(Tools.nowUTC()));

        this.migration.upgrade();

        var migrationCompleted = expectMigrationCompleted();

        assertThat(migrationCompleted.contentPackId()).isNotBlank();
        assertThat(migrationCompleted.installedContentPack()).isFalse();
        assertThat(migrationCompleted.uninstalledPreviousRevision()).isFalse();

        var notification = expectNotificationPublished();

        assertThat(notification.getType()).isEqualTo(Notification.Type.GENERIC);
        assertThat(notification.getSeverity()).isEqualTo(Notification.Severity.NORMAL);
        assertThat(notification.getDetail("title")).isEqualTo("Updating Sources Dashboard");
    }

    MigrationCompleted expectMigrationCompleted() {
        var migrationCompletedCaptor = ArgumentCaptor.forClass(MigrationCompleted.class);
        verify(clusterConfigService, times(1)).write(migrationCompletedCaptor.capture());

        return migrationCompletedCaptor.getValue();
    }

    void previousMigrationHasRun() {
        when(clusterConfigService.get(V20191219090834_AddSourcesPage.MigrationCompleted.class)).thenReturn(V20191219090834_AddSourcesPage.MigrationCompleted.create("04fcf179-49e0-4e8f-9c02-0ff13062efbe"));
    }

    void thisMigrationHasRun() {
        when(clusterConfigService.get(MigrationCompleted.class)).thenReturn(MigrationCompleted.create("deadbeef", true, true));
    }

    void thisMigrationHasNotRun() {
        when(clusterConfigService.get(MigrationCompleted.class)).thenReturn(null);
    }

    Notification expectNotificationPublished() {
        var notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService, times(1)).publishIfFirst(notificationCaptor.capture());

        return notificationCaptor.getValue();
    }
}
