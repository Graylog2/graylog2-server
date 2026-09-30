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
import org.graylog.testing.cluster.ClusterConfigServiceExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.time.ZonedDateTime;
import java.util.List;
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
                notificationService);
        when(notificationService.buildNow()).thenReturn(new NotificationImpl().addTimestamp(Tools.nowUTC()));
    }

    @Test
    void createdAt() {
        assertThat(migration.createdAt()).isEqualTo(ZonedDateTime.parse("2026-09-29T12:00:00Z"));
    }

    @Test
    void publishesNotificationForExistingKinesisInputs() {
        final List<Input> inputs = List.of(input("kinesis-1"), input("kinesis-2"));
        when(inputService.allByType(AWSInput.TYPE)).thenReturn(inputs);

        migration.upgrade();

        final ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).publishIfFirst(captor.capture());
        final Notification notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(Notification.Type.KINESIS_SINGLE_TABLE_MIGRATION);
        assertThat(notification.getSeverity()).isEqualTo(Notification.Severity.NORMAL);

        assertThat(clusterConfigService.get(V20260929120000_NotifyKinesisSingleTableMigration.MigrationCompleted.class))
                .isEqualTo(new V20260929120000_NotifyKinesisSingleTableMigration.MigrationCompleted(
                        Set.of("kinesis-1", "kinesis-2")));
    }

    @Test
    void doesNotNotifyWithoutKinesisInputs() {
        when(inputService.allByType(AWSInput.TYPE)).thenReturn(List.of());

        migration.upgrade();

        verify(notificationService, never()).publishIfFirst(any());
        assertThat(clusterConfigService.get(V20260929120000_NotifyKinesisSingleTableMigration.MigrationCompleted.class))
                .isEqualTo(new V20260929120000_NotifyKinesisSingleTableMigration.MigrationCompleted(Set.of()));
    }

    @Test
    void runsOnlyOnce() {
        clusterConfigService.write(new V20260929120000_NotifyKinesisSingleTableMigration.MigrationCompleted(Set.of()));
        final List<Input> inputs = List.of(input("kinesis-1"));
        when(inputService.allByType(AWSInput.TYPE)).thenReturn(inputs);

        migration.upgrade();

        verify(notificationService, never()).publishIfFirst(any());
    }

    private static Input input(String id) {
        final Input input = mock(Input.class);
        when(input.getId()).thenReturn(id);
        return input;
    }
}
