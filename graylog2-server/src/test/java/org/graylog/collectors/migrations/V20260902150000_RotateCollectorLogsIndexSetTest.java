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
package org.graylog.collectors.migrations;

import org.bson.conversions.Bson;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.MongoIndexSet;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V20260902150000_RotateCollectorLogsIndexSetTest {

    @Mock
    private ClusterConfigService clusterConfigService;
    @Mock
    private IndexSetService indexSetService;
    @Mock
    private MongoIndexSet.Factory mongoIndexSetFactory;
    @Mock
    private IndexSetConfig indexSetConfig;
    @Mock
    private IndexSet indexSet;

    private V20260902150000_RotateCollectorLogsIndexSet migration;

    @BeforeEach
    void setUp() {
        migration = new V20260902150000_RotateCollectorLogsIndexSet(
                clusterConfigService, indexSetService, mongoIndexSetFactory);
    }

    @Test
    void createdAt() {
        assertThat(migration.createdAt()).isEqualTo(ZonedDateTime.parse("2026-09-02T15:00:00Z"));
    }

    @Test
    void doesNothingWhenAlreadyCompleted() {
        when(clusterConfigService.get(V20260902150000_RotateCollectorLogsIndexSet.MigrationCompleted.class))
                .thenReturn(new V20260902150000_RotateCollectorLogsIndexSet.MigrationCompleted(true));

        migration.upgrade();

        verifyNoInteractions(indexSetService, mongoIndexSetFactory);
        verify(clusterConfigService, never()).write(any());
    }

    @Test
    void completesWithoutRotationWhenIndexSetDoesNotExist() {
        when(indexSetService.findOne(any(Bson.class))).thenReturn(Optional.empty());

        migration.upgrade();

        verifyNoInteractions(mongoIndexSetFactory);
        verify(clusterConfigService).write(new V20260902150000_RotateCollectorLogsIndexSet.MigrationCompleted(false));
    }

    @Test
    void rotatesExistingIndexSet() {
        when(indexSetService.findOne(any(Bson.class))).thenReturn(Optional.of(indexSetConfig));
        when(mongoIndexSetFactory.create(indexSetConfig)).thenReturn(indexSet);

        migration.upgrade();

        verify(indexSet).cycle();
        verify(clusterConfigService).write(new V20260902150000_RotateCollectorLogsIndexSet.MigrationCompleted(true));
    }

    @Test
    void retriesOnNextStartupWhenRotationFails() {
        when(indexSetService.findOne(any(Bson.class))).thenReturn(Optional.of(indexSetConfig));
        when(mongoIndexSetFactory.create(indexSetConfig)).thenReturn(indexSet);
        doThrow(new RuntimeException("indexer unavailable")).when(indexSet).cycle();

        // Must not abort server startup, and must not persist the completion marker so the
        // rotation is retried on the next startup.
        assertThatCode(() -> migration.upgrade()).doesNotThrowAnyException();
        verify(clusterConfigService, never()).write(any());
    }
}
