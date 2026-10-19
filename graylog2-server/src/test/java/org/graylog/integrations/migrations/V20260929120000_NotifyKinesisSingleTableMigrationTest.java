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
package org.graylog.integrations.migrations;

import org.graylog.integrations.aws.inputs.AWSInput;
import org.graylog.integrations.aws.transports.KinesisTransport;
import org.graylog.integrations.migrations.V20260929120000_NotifyKinesisSingleTableMigration.MigrationCompleted;
import org.graylog.testing.cluster.ClusterConfigServiceExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.web.customization.Config;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MongoDBExtension.class, ClusterConfigServiceExtension.class})
class V20260929120000_NotifyKinesisSingleTableMigrationTest {
    private final InputService inputService = mock(InputService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private ClusterConfigService clusterConfigService;
    private V20260929120000_NotifyKinesisSingleTableMigration migration;

    @BeforeEach
    void setUp(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
        this.migration = new V20260929120000_NotifyKinesisSingleTableMigration(clusterConfigService, inputService,
                notificationService, new CustomizationConfig(Config.forProductName("Acme Logs")));
        when(notificationService.buildNow()).thenReturn(new NotificationImpl().addTimestamp(Tools.nowUTC()));
    }

    @Test
    void createdAt() {
        assertThat(migration.createdAt()).isEqualTo(ZonedDateTime.parse("2026-09-29T12:00:00Z"));
    }

    @Test
    void publishesNotificationForUnmigratedKinesisInputs() {
        final List<Input> inputs = List.of(input("kinesis-1", false), input("kinesis-2", false));
        when(inputService.allByType(AWSInput.TYPE)).thenReturn(inputs);

        migration.upgrade();

        final ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).publishIfFirst(captor.capture());
        final Notification notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(Notification.Type.KINESIS_SINGLE_TABLE_MIGRATION);
        assertThat(notification.getKey()).isEqualTo(V20260929120000_NotifyKinesisSingleTableMigration.NOTIFICATION_KEY);
        assertThat(notification.getSeverity()).isEqualTo(Notification.Severity.NORMAL);
        assertThat(notification.getDetail(V20260929120000_NotifyKinesisSingleTableMigration.PRODUCT_NAME_DETAIL))
                .isEqualTo("Acme Logs");

        assertThat(clusterConfigService.get(MigrationCompleted.class))
                .isEqualTo(new MigrationCompleted(Set.of("kinesis-1", "kinesis-2")));
    }

    @Test
    void ignoresInputsThatAlreadyUseASingleTable() {
        final List<Input> inputs = List.of(input("kinesis-migrated", true), input("kinesis-legacy", false));
        when(inputService.allByType(AWSInput.TYPE)).thenReturn(inputs);

        migration.upgrade();

        verify(notificationService).publishIfFirst(any());
        assertThat(clusterConfigService.get(MigrationCompleted.class))
                .isEqualTo(new MigrationCompleted(Set.of("kinesis-legacy")));
    }

    @Test
    void doesNotNotifyWhenAllInputsAlreadyUseASingleTable() {
        final List<Input> inputs = List.of(input("kinesis-migrated", true));
        when(inputService.allByType(AWSInput.TYPE)).thenReturn(inputs);

        migration.upgrade();

        verify(notificationService, never()).publishIfFirst(any());
        assertThat(clusterConfigService.get(MigrationCompleted.class)).isEqualTo(new MigrationCompleted(Set.of()));
    }

    @Test
    void countsPre72InputsWithoutTheOption() {
        final List<Input> inputs = List.of(input("kinesis-pre-7.2", Map.of()));
        when(inputService.allByType(AWSInput.TYPE)).thenReturn(inputs);

        migration.upgrade();

        verify(notificationService).publishIfFirst(any());
        assertThat(clusterConfigService.get(MigrationCompleted.class))
                .isEqualTo(new MigrationCompleted(Set.of("kinesis-pre-7.2")));
    }

    @Test
    void treatsStringTrueOptionAsMigrated() {
        final List<Input> inputs = List.of(
                input("kinesis-migrated", Map.of(KinesisTransport.CK_KINESIS_SINGLE_TABLE_STATE_TRACKING, "true")));
        when(inputService.allByType(AWSInput.TYPE)).thenReturn(inputs);

        migration.upgrade();

        verify(notificationService, never()).publishIfFirst(any());
    }

    @Test
    void doesNotNotifyWithoutKinesisInputs() {
        when(inputService.allByType(AWSInput.TYPE)).thenReturn(List.of());

        migration.upgrade();

        verify(notificationService, never()).publishIfFirst(any());
        assertThat(clusterConfigService.get(MigrationCompleted.class)).isEqualTo(new MigrationCompleted(Set.of()));
    }

    @Test
    void runsOnlyOnce() {
        clusterConfigService.write(new MigrationCompleted(Set.of()));

        migration.upgrade();

        verify(inputService, never()).allByType(any());
        verify(notificationService, never()).publishIfFirst(any());
    }

    private static Input input(String id, boolean singleTableEnabled) {
        return input(id, Map.of(KinesisTransport.CK_KINESIS_SINGLE_TABLE_STATE_TRACKING, singleTableEnabled));
    }

    private static Input input(String id, Map<String, Object> configuration) {
        final Input input = mock(Input.class);
        when(input.getId()).thenReturn(id);
        when(input.getConfiguration()).thenReturn(configuration);
        return input;
    }
}
