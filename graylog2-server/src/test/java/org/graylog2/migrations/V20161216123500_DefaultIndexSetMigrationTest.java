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

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.DefaultIndexSetCreated;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.V20161216123500_Succeeded;
import org.graylog2.indexer.indexset.events.IndexSetCreatedEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class V20161216123500_DefaultIndexSetMigrationTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private IndexSetService indexSetService;
    @Mock
    private ClusterConfigService clusterConfigService;
    @Mock
    private ClusterEventBus clusterEventBus;

    private final ElasticsearchConfiguration elasticsearchConfiguration = new ElasticsearchConfiguration();
    private Migration migration;


    @Before
    public void setUpService() throws Exception {
        migration = new V20161216123500_DefaultIndexSetMigration(
                elasticsearchConfiguration,
                indexSetService,
                clusterConfigService,
                clusterEventBus);
    }

    @Test
    public void upgradeCreatesDefaultIndexSet() throws Exception {
        final RotationStrategyConfig rotationStrategyConfig = mock(RotationStrategyConfig.class);
        final RetentionStrategyConfig retentionStrategyConfig = mock(RetentionStrategyConfig.class);
        final IndexSetConfig defaultConfig = IndexSetConfig.builder()
                .id("id")
                .title("title")
                .description("description")
                .indexPrefix("prefix")
                .shards(1)
                .replicas(0)
                .rotationStrategy(rotationStrategyConfig)
                .retentionStrategy(retentionStrategyConfig)
                .creationDate(ZonedDateTime.of(2016, 10, 12, 0, 0, 0, 0, ZoneOffset.UTC))
                .indexAnalyzer("standard")
                .indexTemplateName("prefix-template")
                .indexOptimizationMaxNumSegments(1)
                .indexOptimizationDisabled(false)
                .build();
        final IndexSetConfig savedConfig = defaultConfig.toBuilder()
                .indexAnalyzer(elasticsearchConfiguration.getAnalyzer())
                .indexTemplateName(elasticsearchConfiguration.getTemplateName())
                .indexOptimizationMaxNumSegments(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(elasticsearchConfiguration.isDisableIndexOptimization())
                .build();
        when(indexSetService.save(any(IndexSetConfig.class))).thenReturn(savedConfig);
        when(clusterConfigService.get(DefaultIndexSetCreated.class)).thenReturn(DefaultIndexSetCreated.create());
        when(indexSetService.findAll()).thenReturn(Collections.singletonList(defaultConfig));

        final ArgumentCaptor<IndexSetConfig> indexSetConfigCaptor = ArgumentCaptor.forClass(IndexSetConfig.class);

        migration.upgrade();

        verify(indexSetService).save(indexSetConfigCaptor.capture());
        verify(clusterConfigService).write(V20161216123500_Succeeded.create());
        verify(clusterEventBus).post(IndexSetCreatedEvent.create(savedConfig));

        final IndexSetConfig capturedIndexSetConfig = indexSetConfigCaptor.getValue();
        assertThat(capturedIndexSetConfig.id()).isEqualTo("id");
        assertThat(capturedIndexSetConfig.title()).isEqualTo("title");
        assertThat(capturedIndexSetConfig.description()).isEqualTo("description");
        assertThat(capturedIndexSetConfig.indexPrefix()).isEqualTo("prefix");
        assertThat(capturedIndexSetConfig.shards()).isEqualTo(1);
        assertThat(capturedIndexSetConfig.replicas()).isEqualTo(0);
        assertThat(capturedIndexSetConfig.rotationStrategy()).isEqualTo(rotationStrategyConfig);
        assertThat(capturedIndexSetConfig.retentionStrategy()).isEqualTo(retentionStrategyConfig);
        assertThat(capturedIndexSetConfig.indexAnalyzer()).isEqualTo(elasticsearchConfiguration.getAnalyzer());
        assertThat(capturedIndexSetConfig.indexTemplateName()).isEqualTo(elasticsearchConfiguration.getTemplateName());
        assertThat(capturedIndexSetConfig.indexOptimizationMaxNumSegments()).isEqualTo(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments());
        assertThat(capturedIndexSetConfig.indexOptimizationDisabled()).isEqualTo(elasticsearchConfiguration.isDisableIndexOptimization());
    }

    @Test
    public void upgradeFailsIfDefaultIndexSetHasNotBeenCreated() throws Exception {
        when(clusterConfigService.get(DefaultIndexSetCreated.class)).thenReturn(null);
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("The default index set hasn't been created yet. This is a bug!");

        migration.upgrade();
    }

    @Test
    public void migrationDoesNotRunAgainIfMigrationWasSuccessfulBefore() throws Exception {
        when(clusterConfigService.get(V20161216123500_Succeeded.class)).thenReturn(V20161216123500_Succeeded.create());
        migration.upgrade();

        verify(clusterConfigService).get(V20161216123500_Succeeded.class);
        verifyNoMoreInteractions(clusterConfigService);
        verifyZeroInteractions(clusterEventBus);
        verifyZeroInteractions(indexSetService);
    }
}