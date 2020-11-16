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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.ZonedDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class V20161122174500_AssignIndexSetsToStreamsMigrationTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private IndexSetService indexSetService;
    @Mock
    private StreamService streamService;
    @Mock
    private ClusterConfigService clusterConfigService;

    private Migration migration;

    @Before
    public void setUp() throws Exception {
        migration = new V20161122174500_AssignIndexSetsToStreamsMigration(streamService, indexSetService, clusterConfigService);
    }

    @Test
    public void createdAt() throws Exception {
        // Test the date to detect accidental changes to it.
        assertThat(migration.createdAt()).isEqualTo(ZonedDateTime.parse("2016-11-22T17:45:00Z"));
    }

    @Test
    public void upgrade() throws Exception {
        final Stream stream1 = mock(Stream.class);
        final Stream stream2 = mock(Stream.class);
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);

        when(indexSetService.findAll()).thenReturn(Collections.singletonList(indexSetConfig));
        when(indexSetConfig.id()).thenReturn("abc123");
        when(stream1.getId()).thenReturn("stream1");
        when(stream2.getId()).thenReturn("stream2");
        when(streamService.loadAll()).thenReturn(Lists.newArrayList(stream1, stream2));

        migration.upgrade();

        verify(stream1).setIndexSetId(indexSetConfig.id());
        verify(stream2).setIndexSetId(indexSetConfig.id());
        verify(streamService, times(1)).save(stream1);
        verify(streamService, times(1)).save(stream2);
        verify(clusterConfigService, times(1)).write(
                V20161122174500_AssignIndexSetsToStreamsMigration.MigrationCompleted.create(
                        indexSetConfig.id(), Sets.newHashSet("stream1", "stream2"), Collections.emptySet()));
    }

    @Test
    public void upgradeWithAlreadyAssignedIndexSet() throws Exception {
        final Stream stream1 = mock(Stream.class);
        final Stream stream2 = mock(Stream.class);
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);

        when(indexSetService.findAll()).thenReturn(Collections.singletonList(indexSetConfig));
        when(indexSetConfig.id()).thenReturn("abc123");
        when(stream1.getId()).thenReturn("stream1");
        when(stream2.getId()).thenReturn("stream2");
        when(streamService.loadAll()).thenReturn(Lists.newArrayList(stream1, stream2));
        when(stream2.getIndexSetId()).thenReturn("abc123");

        migration.upgrade();

        verify(stream1).setIndexSetId(indexSetConfig.id());
        verify(stream2, never()).setIndexSetId(indexSetConfig.id());
        verify(streamService, times(1)).save(stream1);
        verify(streamService, never()).save(stream2);
        verify(clusterConfigService, times(1)).write(
                V20161122174500_AssignIndexSetsToStreamsMigration.MigrationCompleted.create(
                        indexSetConfig.id(), Sets.newHashSet("stream1"), Collections.emptySet()));
    }

    @Test
    public void upgradeWithFailedStreamUpdate() throws Exception {
        final Stream stream1 = mock(Stream.class);
        final Stream stream2 = mock(Stream.class);
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);

        when(indexSetService.findAll()).thenReturn(Collections.singletonList(indexSetConfig));
        when(indexSetConfig.id()).thenReturn("abc123");
        when(stream1.getId()).thenReturn("stream1");
        when(stream2.getId()).thenReturn("stream2");
        when(streamService.loadAll()).thenReturn(Lists.newArrayList(stream1, stream2));

        // Updating stream1 should fail!
        when(streamService.save(stream1)).thenThrow(ValidationException.class);

        migration.upgrade();

        verify(stream1).setIndexSetId(indexSetConfig.id());
        verify(stream2).setIndexSetId(indexSetConfig.id());
        verify(streamService, times(1)).save(stream1);
        verify(streamService, times(1)).save(stream2);

        // Check that the failed stream1 will be recorded as failed!
        verify(clusterConfigService, times(1)).write(
                V20161122174500_AssignIndexSetsToStreamsMigration.MigrationCompleted.create(
                        indexSetConfig.id(), Sets.newHashSet("stream2"), Sets.newHashSet("stream1")));
    }

    @Test
    public void upgradeWithoutAnyIndexSetConfig() throws Exception {
        when(indexSetService.findAll()).thenReturn(Collections.emptyList());

        expectedException.expect(IllegalStateException.class);

        migration.upgrade();
    }

    @Test
    public void upgradeWithMoreThanOneIndexSetConfig() throws Exception {
        when(indexSetService.findAll()).thenReturn(Lists.newArrayList(mock(IndexSetConfig.class), mock(IndexSetConfig.class)));

        expectedException.expect(IllegalStateException.class);

        migration.upgrade();
    }

    @Test
    public void upgradeWhenAlreadyCompleted() throws Exception {
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);

        when(indexSetService.findAll()).thenReturn(Collections.singletonList(indexSetConfig));
        when(indexSetConfig.id()).thenReturn("abc123");
        when(clusterConfigService.get(V20161122174500_AssignIndexSetsToStreamsMigration.MigrationCompleted.class))
                .thenReturn(V20161122174500_AssignIndexSetsToStreamsMigration.MigrationCompleted.create("1", Collections.emptySet(), Collections.emptySet()));

        migration.upgrade();

        verify(streamService, never()).save(any(Stream.class));
        verify(clusterConfigService, never()).write(any(V20161122174500_AssignIndexSetsToStreamsMigration.MigrationCompleted.class));
    }
}