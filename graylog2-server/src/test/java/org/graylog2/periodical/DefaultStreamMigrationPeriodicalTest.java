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

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.management.IndexManagementConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamRuleService;
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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
    private StreamRuleService streamRuleService;
    @Mock
    private ClusterEventBus clusterEventBus;
    @Mock
    private ClusterConfigService clusterConfigService;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private IndexSetService indexSetService;

    private final ElasticsearchConfiguration elasticsearchConfiguration = new ElasticsearchConfiguration();
    private RotationStrategy rotationStrategy = new StubRotationStrategy();
    private RetentionStrategy retentionStrategy = new StubRetentionStrategy();

    @Before
    public void setUpService() throws Exception {
        periodical = new DefaultStreamMigrationPeriodical(streamService,
                streamRuleService,
                elasticsearchConfiguration,
                Collections.singletonMap("test", () -> rotationStrategy),
                Collections.singletonMap("test", () -> retentionStrategy),
                indexSetService,
                clusterEventBus,
                clusterConfigService);
    }

    @Test
    public void doRunCreatesStreamAndStreamRule() throws Exception {
        final ArgumentCaptor<Stream> streamArgumentCaptor = ArgumentCaptor.forClass(Stream.class);

        periodical.doRunDefaultStreamMigration();

        verify(streamService).save(streamArgumentCaptor.capture());

        final Stream stream = streamArgumentCaptor.getValue();
        assertThat(stream.getTitle()).isEqualTo("All messages");
        assertThat(stream.getDisabled()).isFalse();
        assertThat(stream.getMatchingType()).isEqualTo(StreamImpl.MatchingType.DEFAULT);
    }

    @Test
    public void doRunPostsStreamsChangedEvent() throws Exception {
        final ArgumentCaptor<StreamsChangedEvent> argumentCaptor = ArgumentCaptor.forClass(StreamsChangedEvent.class);
        periodical.doRunDefaultStreamMigration();
        verify(clusterEventBus).post(argumentCaptor.capture());

        final StreamsChangedEvent streamsChangedEvent = argumentCaptor.getValue();
        assertThat(streamsChangedEvent.streamIds()).containsOnly(Stream.DEFAULT_STREAM_ID);
    }

    @Test
    public void doRunDoesNotCreateStreamRuleIfStreamCreationFails() throws Exception {
        when(streamService.save(any(Stream.class))).thenThrow(ValidationException.class);

        periodical.doRunDefaultStreamMigration();

        verifyNoMoreInteractions(streamRuleService);
    }

    @Test
    public void doRunDoesNotPostStreamsChangedEventIfStreamCreationFails() throws Exception {
        when(streamService.save(any(Stream.class))).thenThrow(ValidationException.class);

        periodical.doRunDefaultStreamMigration();

        verifyNoMoreInteractions(clusterEventBus);
    }

    @Test
    public void doRunDoesNotSaveDefaultStreamCreatedClusterConfigIfStreamCreationFails() throws Exception {
        when(streamService.save(any(Stream.class))).thenThrow(ValidationException.class);

        periodical.doRunDefaultStreamMigration();

        verifyNoMoreInteractions(clusterConfigService);
    }

    @Test
    public void doRunCreatesDefaultIndexSet() throws Exception {
        final StubRotationStrategyConfig rotationStrategyConfig = new StubRotationStrategyConfig();
        final StubRetentionStrategyConfig retentionStrategyConfig = new StubRetentionStrategyConfig();
        final IndexSetConfig savedIndexSetConfig = IndexSetConfig.builder()
                .id("id")
                .title("title")
                .indexPrefix("prefix")
                .shards(1)
                .replicas(0)
                .rotationStrategy(rotationStrategyConfig)
                .retentionStrategy(retentionStrategyConfig)
                .creationDate(ZonedDateTime.of(2016, 10, 12, 0, 0, 0, 0, ZoneOffset.UTC))
                .build();
        when(clusterConfigService.get(IndexManagementConfig.class)).thenReturn(IndexManagementConfig.create("test", "test"));
        when(clusterConfigService.get(StubRotationStrategyConfig.class)).thenReturn(rotationStrategyConfig);
        when(clusterConfigService.get(StubRetentionStrategyConfig.class)).thenReturn(retentionStrategyConfig);
        when(indexSetService.save(any(IndexSetConfig.class))).thenReturn(savedIndexSetConfig);

        final ArgumentCaptor<IndexSetConfig> indexSetConfigCaptor = ArgumentCaptor.forClass(IndexSetConfig.class);

        periodical.doRunIndexSetMigration();

        verify(indexSetService).save(indexSetConfigCaptor.capture());
        verify(clusterConfigService).write(DefaultStreamMigrationPeriodical.IndexSetMigrated.create());

        final IndexSetConfig capturedIndexSetConfig = indexSetConfigCaptor.getValue();
        assertThat(capturedIndexSetConfig.id()).isNull();
        assertThat(capturedIndexSetConfig.title()).isEqualTo("Default index set");
        assertThat(capturedIndexSetConfig.description()).isEqualTo("The Graylog default index set");
        assertThat(capturedIndexSetConfig.indexPrefix()).isEqualTo(elasticsearchConfiguration.getIndexPrefix());
        assertThat(capturedIndexSetConfig.shards()).isEqualTo(elasticsearchConfiguration.getShards());
        assertThat(capturedIndexSetConfig.replicas()).isEqualTo(elasticsearchConfiguration.getReplicas());
        assertThat(capturedIndexSetConfig.rotationStrategy()).isInstanceOf(StubRotationStrategyConfig.class);
        assertThat(capturedIndexSetConfig.retentionStrategy()).isInstanceOf(StubRetentionStrategyConfig.class);
    }

    @Test
    public void doRunCreatesDefaultIndexSetWithDefaultRotationAndRetentionStrategyConfig() throws Exception {
        final StubRotationStrategyConfig rotationStrategyConfig = new StubRotationStrategyConfig();
        final StubRetentionStrategyConfig retentionStrategyConfig = new StubRetentionStrategyConfig();
        final IndexSetConfig savedIndexSetConfig = IndexSetConfig.builder()
                .id("id")
                .title("title")
                .indexPrefix("prefix")
                .shards(1)
                .replicas(0)
                .rotationStrategy(rotationStrategyConfig)
                .retentionStrategy(retentionStrategyConfig)
                .creationDate(ZonedDateTime.of(2016, 10, 12, 0, 0, 0, 0, ZoneOffset.UTC))
                .build();
        when(clusterConfigService.get(IndexManagementConfig.class)).thenReturn(IndexManagementConfig.create("test", "test"));
        when(clusterConfigService.get(StubRotationStrategyConfig.class)).thenReturn(null);
        when(clusterConfigService.get(StubRetentionStrategyConfig.class)).thenReturn(null);
        when(indexSetService.save(any(IndexSetConfig.class))).thenReturn(savedIndexSetConfig);

        final ArgumentCaptor<IndexSetConfig> indexSetConfigCaptor = ArgumentCaptor.forClass(IndexSetConfig.class);

        periodical.doRunIndexSetMigration();

        verify(indexSetService).save(indexSetConfigCaptor.capture());
        verify(clusterConfigService).write(DefaultStreamMigrationPeriodical.IndexSetMigrated.create());

        final IndexSetConfig capturedIndexSetConfig = indexSetConfigCaptor.getValue();
        assertThat(capturedIndexSetConfig.rotationStrategy()).isInstanceOf(StubRotationStrategyConfig.class);
        assertThat(capturedIndexSetConfig.retentionStrategy()).isInstanceOf(StubRetentionStrategyConfig.class);
    }

    @Test
    public void doRunThrowsIllegalStateExceptionIfIndexManagementConfigIsMissing() throws Exception {
        when(clusterConfigService.get(IndexManagementConfig.class)).thenReturn(null);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Couldn't find index management configuration");

        periodical.doRunIndexSetMigration();
    }

    @Test
    public void doRunThrowsIllegalStateExceptionIfRotationStrategyIsMissing() throws Exception {
        when(clusterConfigService.get(IndexManagementConfig.class)).thenReturn(IndexManagementConfig.create("foobar", "test"));

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Couldn't retrieve rotation strategy provider for <foobar>");

        periodical.doRunIndexSetMigration();
    }

    @Test
    public void doRunThrowsIllegalStateExceptionIfRetentionStrategyIsMissing() throws Exception {
        when(clusterConfigService.get(IndexManagementConfig.class)).thenReturn(IndexManagementConfig.create("test", "foobar"));

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Couldn't retrieve retention strategy provider for <foobar>");

        periodical.doRunIndexSetMigration();
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
    public void testDoRunIfDefaultStreamHasBeenCreatedBefore() throws Exception {
        final DefaultStreamMigrationPeriodical periodicalSpy = spy(periodical);

        final Stream stream = mock(Stream.class);
        when(streamService.load(Stream.DEFAULT_STREAM_ID)).thenReturn(stream);
        doNothing().when(periodicalSpy).doRunDefaultStreamMigration();
        doNothing().when(periodicalSpy).doRunIndexSetMigration();

        periodicalSpy.doRun();

        verify(periodicalSpy, never()).doRunDefaultStreamMigration();
    }

    @Test
    public void testDoRunIfDefaultStreamDoesNotExist() throws Exception {
        final DefaultStreamMigrationPeriodical periodicalSpy = spy(periodical);

        when(streamService.load("000000000000000000000001")).thenThrow(NotFoundException.class);
        doNothing().when(periodicalSpy).doRunDefaultStreamMigration();
        doNothing().when(periodicalSpy).doRunIndexSetMigration();

        periodicalSpy.doRun();

        verify(periodicalSpy, times(1)).doRunDefaultStreamMigration();
    }

    @Test
    public void testDoRunIfIndexSetHasBeenCreated() throws Exception {
        final DefaultStreamMigrationPeriodical periodicalSpy = spy(periodical);

        when(clusterConfigService.get(DefaultStreamMigrationPeriodical.IndexSetMigrated.class)).thenReturn(DefaultStreamMigrationPeriodical.IndexSetMigrated.create());
        doNothing().when(periodicalSpy).doRunDefaultStreamMigration();
        doNothing().when(periodicalSpy).doRunIndexSetMigration();

        periodicalSpy.doRun();

        verify(periodicalSpy, never()).doRunIndexSetMigration();
    }

    @Test
    public void testDoRunIfIndexSetHasNotBeenCreated() throws Exception {
        final DefaultStreamMigrationPeriodical periodicalSpy = spy(periodical);

        when(clusterConfigService.get(DefaultStreamMigrationPeriodical.IndexSetMigrated.class)).thenReturn(null);
        doNothing().when(periodicalSpy).doRunDefaultStreamMigration();
        doNothing().when(periodicalSpy).doRunIndexSetMigration();

        periodicalSpy.doRun();

        verify(periodicalSpy, times(1)).doRunIndexSetMigration();
    }

    @Test
    public void startOnThisNodeReturnsTrue() throws Exception {
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

    private static class StubRotationStrategy implements RotationStrategy {
        @Override
        public void rotate(IndexSet indexSet) {
        }

        @Override
        public Class<? extends RotationStrategyConfig> configurationClass() {
            return StubRotationStrategyConfig.class;
        }

        @Override
        public RotationStrategyConfig defaultConfiguration() {
            return new StubRotationStrategyConfig();
        }
    }

    private static class StubRotationStrategyConfig implements RotationStrategyConfig {
        @Override
        public String type() {
            return StubRotationStrategy.class.getCanonicalName();
        }
    }

    private static class StubRetentionStrategy implements RetentionStrategy {
        @Override
        public void retain(IndexSet indexSet) {
        }

        @Override
        public Class<? extends RetentionStrategyConfig> configurationClass() {
            return StubRetentionStrategyConfig.class;
        }

        @Override
        public RetentionStrategyConfig defaultConfiguration() {
            return new StubRetentionStrategyConfig();
        }
    }

    private static class StubRetentionStrategyConfig implements RetentionStrategyConfig {
        @Override
        public String type() {
            return StubRetentionStrategy.class.getCanonicalName();
        }
    }
}
