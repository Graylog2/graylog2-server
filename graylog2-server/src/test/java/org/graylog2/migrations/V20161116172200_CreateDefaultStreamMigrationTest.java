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

import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class V20161116172200_CreateDefaultStreamMigrationTest {

    private Migration migration;
    @Mock
    private StreamService streamService;
    @Mock
    private IndexSetRegistry indexSetRegistry;
    @Mock
    private IndexSet indexSet;
    @Mock
    private IndexSetConfig indexSetConfig;

    @BeforeEach
    public void setUpService() {
        migration = new V20161116172200_CreateDefaultStreamMigration(streamService, indexSetRegistry);

        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.id()).thenReturn("abc123");
    }

    @Test
    public void upgrade() throws Exception {
        final ArgumentCaptor<Stream> streamArgumentCaptor = ArgumentCaptor.forClass(Stream.class);
        when(streamService.load("000000000000000000000001")).thenThrow(NotFoundException.class);
        when(indexSetRegistry.getDefault()).thenReturn(indexSet);

        migration.upgrade();

        verify(streamService).save(streamArgumentCaptor.capture());

        final Stream stream = streamArgumentCaptor.getValue();
        assertThat(stream.getTitle()).isEqualTo("Default Stream");
        assertThat(stream.getDisabled()).isFalse();
        assertThat(stream.getMatchingType()).isEqualTo(StreamImpl.MatchingType.DEFAULT);
    }

    @Test
    public void upgradeWithoutDefaultIndexSet() throws Exception {
        when(streamService.load("000000000000000000000001")).thenThrow(NotFoundException.class);
        when(indexSetRegistry.getDefault()).thenThrow(IllegalStateException.class);

        assertThrows(IllegalStateException.class, () ->

            migration.upgrade());
    }

    @Test
    public void upgradeDoesNotRunIfDefaultStreamExists() throws Exception {
        when(streamService.load("000000000000000000000001")).thenReturn(mock(Stream.class));

        migration.upgrade();

        verify(streamService, never()).save(any(Stream.class));
    }
}
