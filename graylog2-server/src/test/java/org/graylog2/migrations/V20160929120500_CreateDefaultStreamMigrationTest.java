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
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.events.StreamsChangedEvent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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

public class V20160929120500_CreateDefaultStreamMigrationTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private Migration migration;
    @Mock
    private StreamService streamService;
    @Mock
    private ClusterEventBus clusterEventBus;

    @Before
    public void setUpService() throws Exception {
        migration = new V20160929120500_CreateDefaultStreamMigration(streamService, clusterEventBus);
    }

    @Test
    public void upgrade() throws Exception {
        final ArgumentCaptor<Stream> streamArgumentCaptor = ArgumentCaptor.forClass(Stream.class);
        when(streamService.load("000000000000000000000001")).thenThrow(NotFoundException.class);

        migration.upgrade();

        verify(streamService).saveWithoutValidation(streamArgumentCaptor.capture());

        final Stream stream = streamArgumentCaptor.getValue();
        assertThat(stream.getTitle()).isEqualTo("All messages");
        assertThat(stream.getDisabled()).isFalse();
        assertThat(stream.getMatchingType()).isEqualTo(StreamImpl.MatchingType.DEFAULT);
    }

    @Test
    public void upgradeDoesNotRunIfDefaultStreamExists() throws Exception {
        when(streamService.load("000000000000000000000001")).thenReturn(mock(Stream.class));

        migration.upgrade();

        verify(streamService, never()).save(any(Stream.class));
    }

    @Test
    public void upgradePostsStreamsChangedEvent() throws Exception {
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