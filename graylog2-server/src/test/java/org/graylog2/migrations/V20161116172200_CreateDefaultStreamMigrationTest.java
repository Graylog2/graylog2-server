/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.migrations;

import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.events.StreamsChangedEvent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class V20161116172200_CreateDefaultStreamMigrationTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private Migration migration;
    @Mock
    private StreamService streamService;
    @Mock
    private ClusterEventBus clusterEventBus;
    @Mock
    private IndexSetRegistry indexSetRegistry;
    @Mock
    private IndexSet indexSet;
    @Mock
    private IndexSetConfig indexSetConfig;

    @Before
    public void setUpService() throws Exception {
        migration = new V20161116172200_CreateDefaultStreamMigration(streamService, clusterEventBus, indexSetRegistry);

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
        assertThat(stream.getTitle()).isEqualTo("All messages");
        assertThat(stream.getDisabled()).isFalse();
        assertThat(stream.getMatchingType()).isEqualTo(StreamImpl.MatchingType.DEFAULT);
    }

    @Test
    public void upgradeWithoutDefaultIndexSet() throws Exception {
        when(streamService.load("000000000000000000000001")).thenThrow(NotFoundException.class);
        when(indexSetRegistry.getDefault()).thenThrow(IllegalStateException.class);

        expectedException.expect(IllegalStateException.class);

        migration.upgrade();
    }

    @Test
    public void upgradeDoesNotRunIfDefaultStreamExists() throws Exception {
        when(streamService.load("000000000000000000000001")).thenReturn(mock(Stream.class));

        migration.upgrade();

        verify(streamService, never()).save(any(Stream.class));
    }

    @Test
    public void upgradePostsStreamsChangedEvent() throws Exception {
        when(indexSetRegistry.getDefault()).thenReturn(indexSet);
        when(streamService.load("000000000000000000000001")).thenThrow(NotFoundException.class);
        final ArgumentCaptor<StreamsChangedEvent> argumentCaptor = ArgumentCaptor.forClass(StreamsChangedEvent.class);
        migration.upgrade();
        verify(clusterEventBus).post(argumentCaptor.capture());

        final StreamsChangedEvent streamsChangedEvent = argumentCaptor.getValue();
        assertThat(streamsChangedEvent.streamIds()).containsOnly(Stream.DEFAULT_STREAM_ID);
    }

    @Test
    public void upgradeDoesNotPostStreamsChangedEventIfStreamCreationFails() throws Exception {
        when(streamService.save(any(Stream.class))).thenThrow(ValidationException.class);

        migration.upgrade();

        verifyNoMoreInteractions(clusterEventBus);
    }
}