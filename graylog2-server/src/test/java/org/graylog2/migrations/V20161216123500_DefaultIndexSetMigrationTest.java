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

import com.google.common.collect.ImmutableList;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.indexset.DefaultIndexSetCreated;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.V20161216123500_Succeeded;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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

    private final ElasticsearchConfiguration elasticsearchConfiguration = new ElasticsearchConfiguration();
    private Migration migration;


    @Before
    public void setUpService() {
        migration = new V20161216123500_DefaultIndexSetMigration(
                elasticsearchConfiguration,
                indexSetService,
                clusterConfigService);
    }

    @Test
    @SuppressWarnings("deprecation")
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
        final IndexSetConfig additionalConfig = defaultConfig.toBuilder()
                .id("foo")
                .indexPrefix("foo")
                .build();
        final IndexSetConfig savedDefaultConfig = defaultConfig.toBuilder()
                .indexAnalyzer(elasticsearchConfiguration.getAnalyzer())
                .indexTemplateName(elasticsearchConfiguration.getTemplateName())
                .indexOptimizationMaxNumSegments(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(elasticsearchConfiguration.isDisableIndexOptimization())
                .build();
        final IndexSetConfig savedAdditionalConfig = additionalConfig.toBuilder()
                .indexAnalyzer(elasticsearchConfiguration.getAnalyzer())
                .indexTemplateName("foo-template")
                .indexOptimizationMaxNumSegments(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(elasticsearchConfiguration.isDisableIndexOptimization())
                .build();

        when(indexSetService.save(any(IndexSetConfig.class))).thenReturn(savedAdditionalConfig, savedDefaultConfig);
        when(indexSetService.getDefault()).thenReturn(defaultConfig);
        when(indexSetService.findAll()).thenReturn(ImmutableList.of(defaultConfig, additionalConfig));
        when(clusterConfigService.get(DefaultIndexSetCreated.class)).thenReturn(DefaultIndexSetCreated.create());

        final ArgumentCaptor<IndexSetConfig> indexSetConfigCaptor = ArgumentCaptor.forClass(IndexSetConfig.class);

        migration.upgrade();

        verify(indexSetService, times(2)).save(indexSetConfigCaptor.capture());
        verify(clusterConfigService).write(V20161216123500_Succeeded.create());

        final List<IndexSetConfig> allValues = indexSetConfigCaptor.getAllValues();
        assertThat(allValues).hasSize(2);

        final IndexSetConfig capturedDefaultIndexSetConfig = allValues.get(0);
        assertThat(capturedDefaultIndexSetConfig.id()).isEqualTo("id");
        assertThat(capturedDefaultIndexSetConfig.title()).isEqualTo("title");
        assertThat(capturedDefaultIndexSetConfig.description()).isEqualTo("description");
        assertThat(capturedDefaultIndexSetConfig.indexPrefix()).isEqualTo("prefix");
        assertThat(capturedDefaultIndexSetConfig.shards()).isEqualTo(1);
        assertThat(capturedDefaultIndexSetConfig.replicas()).isEqualTo(0);
        assertThat(capturedDefaultIndexSetConfig.rotationStrategy()).isEqualTo(rotationStrategyConfig);
        assertThat(capturedDefaultIndexSetConfig.retentionStrategy()).isEqualTo(retentionStrategyConfig);
        assertThat(capturedDefaultIndexSetConfig.indexAnalyzer()).isEqualTo(elasticsearchConfiguration.getAnalyzer());
        assertThat(capturedDefaultIndexSetConfig.indexTemplateName()).isEqualTo(elasticsearchConfiguration.getTemplateName());
        assertThat(capturedDefaultIndexSetConfig.indexOptimizationMaxNumSegments()).isEqualTo(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments());
        assertThat(capturedDefaultIndexSetConfig.indexOptimizationDisabled()).isEqualTo(elasticsearchConfiguration.isDisableIndexOptimization());

        final IndexSetConfig capturedAdditionalIndexSetConfig = allValues.get(1);
        assertThat(capturedAdditionalIndexSetConfig.id()).isEqualTo("foo");
        assertThat(capturedAdditionalIndexSetConfig.title()).isEqualTo("title");
        assertThat(capturedAdditionalIndexSetConfig.description()).isEqualTo("description");
        assertThat(capturedAdditionalIndexSetConfig.indexPrefix()).isEqualTo("foo");
        assertThat(capturedAdditionalIndexSetConfig.shards()).isEqualTo(1);
        assertThat(capturedAdditionalIndexSetConfig.replicas()).isEqualTo(0);
        assertThat(capturedAdditionalIndexSetConfig.rotationStrategy()).isEqualTo(rotationStrategyConfig);
        assertThat(capturedAdditionalIndexSetConfig.retentionStrategy()).isEqualTo(retentionStrategyConfig);
        assertThat(capturedAdditionalIndexSetConfig.indexAnalyzer()).isEqualTo(elasticsearchConfiguration.getAnalyzer());
        assertThat(capturedAdditionalIndexSetConfig.indexTemplateName()).isEqualTo("foo-template");
        assertThat(capturedAdditionalIndexSetConfig.indexOptimizationMaxNumSegments()).isEqualTo(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments());
        assertThat(capturedAdditionalIndexSetConfig.indexOptimizationDisabled()).isEqualTo(elasticsearchConfiguration.isDisableIndexOptimization());
    }

    @Test
    public void upgradeFailsIfDefaultIndexSetHasNotBeenCreated() {
        when(clusterConfigService.get(DefaultIndexSetCreated.class)).thenReturn(null);
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("The default index set hasn't been created yet. This is a bug!");

        migration.upgrade();
    }

    @Test
    public void migrationDoesNotRunAgainIfMigrationWasSuccessfulBefore() {
        when(clusterConfigService.get(V20161216123500_Succeeded.class)).thenReturn(V20161216123500_Succeeded.create());
        migration.upgrade();

        verify(clusterConfigService).get(V20161216123500_Succeeded.class);
        verifyNoMoreInteractions(clusterConfigService);
        verifyZeroInteractions(indexSetService);
    }
}
