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
package org.graylog2.periodical;

import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DefaultStreamMigrationPeriodicalTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private DefaultStreamMigrationPeriodical periodical;
    @Mock
    private StreamService streamService;
    @Mock
    private ClusterEventBus clusterEventBus;

    @Before
    public void setUpService() throws Exception {
        periodical = new DefaultStreamMigrationPeriodical(streamService, clusterEventBus);
    }

    @Test
    public void doRunCreatesStreamAndStreamRule() throws Exception {
        final ArgumentCaptor<Stream> streamArgumentCaptor = ArgumentCaptor.forClass(Stream.class);

        periodical.doRun();

        verify(streamService).save(streamArgumentCaptor.capture());

        final Stream stream = streamArgumentCaptor.getValue();
        assertThat(stream.getTitle()).isEqualTo("All messages");
        assertThat(stream.getDisabled()).isFalse();
        assertThat(stream.getMatchingType()).isEqualTo(StreamImpl.MatchingType.DEFAULT);
    }

    @Test
    public void doRunPostsStreamsChangedEvent() throws Exception {
        final ArgumentCaptor<StreamsChangedEvent> argumentCaptor = ArgumentCaptor.forClass(StreamsChangedEvent.class);
        periodical.doRun();
        verify(clusterEventBus).post(argumentCaptor.capture());

        final StreamsChangedEvent streamsChangedEvent = argumentCaptor.getValue();
        assertThat(streamsChangedEvent.streamIds()).containsOnly(Stream.DEFAULT_STREAM_ID);
    }

    @Test
    public void doRunDoesNotCreateStreamRuleIfStreamCreationFails() throws Exception {
        when(streamService.save(any(Stream.class))).thenThrow(ValidationException.class);

        periodical.doRun();
    }

    @Test
    public void doRunDoesNotPostStreamsChangedEventIfStreamCreationFails() throws Exception {
        when(streamService.save(any(Stream.class))).thenThrow(ValidationException.class);

        periodical.doRun();

        verifyNoMoreInteractions(clusterEventBus);
    }

    @Test
    public void runsForeverReturnsTrue() throws Exception {
        assertThat(periodical.runsForever()).isTrue();
    }

    @Test
    public void stopOnGracefulShutdownReturnsFalse() throws Exception {
        assertThat(periodical.stopOnGracefulShutdown()).isFalse();
    }

    @Test
    public void masterOnlyReturnsTrue() throws Exception {
        assertThat(periodical.masterOnly()).isTrue();
    }

    @Test
    public void startOnThisNodeReturnsFalseIfDefaultStreamHasBeenCreatedBefore() throws Exception {
        final Stream stream = mock(Stream.class);
        when(streamService.load(Stream.DEFAULT_STREAM_ID)).thenReturn(stream);
        assertThat(periodical.startOnThisNode()).isFalse();
    }

    @Test
    public void startOnThisNodeReturnsTrueIfStreamDoesNotExist() throws Exception {
        when(streamService.load("000000000000000000000001")).thenThrow(NotFoundException.class);
        assertThat(periodical.startOnThisNode()).isTrue();
    }

    @Test
    public void isDaemonReturnsFalse() throws Exception {
        assertThat(periodical.isDaemon()).isFalse();
    }

    @Test
    public void getInitialDelaySecondsReturns0() throws Exception {
        assertThat(periodical.getInitialDelaySeconds()).isEqualTo(0);
    }

    @Test
    public void getPeriodSecondsReturns0() throws Exception {
        assertThat(periodical.getPeriodSeconds()).isEqualTo(0);
    }

    @Test
    public void getLoggerReturnsClassLogger() throws Exception {
        assertThat(periodical.getLogger().getName()).isEqualTo(DefaultStreamMigrationPeriodical.class.getCanonicalName());
    }
}
