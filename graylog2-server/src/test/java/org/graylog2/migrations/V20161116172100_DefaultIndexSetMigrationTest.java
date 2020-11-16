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

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.DefaultIndexSetConfig;
import org.graylog2.indexer.indexset.DefaultIndexSetCreated;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.management.IndexManagementConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class V20161116172100_DefaultIndexSetMigrationTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private IndexSetService indexSetService;
    @Mock
    private ClusterConfigService clusterConfigService;

    private final ElasticsearchConfiguration elasticsearchConfiguration = new ElasticsearchConfiguration();
    private RotationStrategy rotationStrategy = new StubRotationStrategy();
    private RetentionStrategy retentionStrategy = new StubRetentionStrategy();
    private Migration migration;


    @Before
    public void setUpService() throws Exception {
        migration = new V20161116172100_DefaultIndexSetMigration(
                elasticsearchConfiguration,
                Collections.singletonMap("test", () -> rotationStrategy),
                Collections.singletonMap("test", () -> retentionStrategy),
                indexSetService,
                clusterConfigService);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void upgradeCreatesDefaultIndexSet() throws Exception {
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
                .indexAnalyzer("standard")
                .indexTemplateName("prefix-template")
                .indexOptimizationMaxNumSegments(1)
                .indexOptimizationDisabled(false)
                .build();
        when(clusterConfigService.get(IndexManagementConfig.class)).thenReturn(IndexManagementConfig.create("test", "test"));
        when(clusterConfigService.get(StubRotationStrategyConfig.class)).thenReturn(rotationStrategyConfig);
        when(clusterConfigService.get(StubRetentionStrategyConfig.class)).thenReturn(retentionStrategyConfig);
        when(indexSetService.save(any(IndexSetConfig.class))).thenReturn(savedIndexSetConfig);

        final ArgumentCaptor<IndexSetConfig> indexSetConfigCaptor = ArgumentCaptor.forClass(IndexSetConfig.class);

        migration.upgrade();

        verify(indexSetService).save(indexSetConfigCaptor.capture());
        verify(clusterConfigService).write(DefaultIndexSetConfig.create("id"));
        verify(clusterConfigService).write(DefaultIndexSetCreated.create());

        final IndexSetConfig capturedIndexSetConfig = indexSetConfigCaptor.getValue();
        assertThat(capturedIndexSetConfig.id()).isNull();
        assertThat(capturedIndexSetConfig.title()).isEqualTo("Default index set");
        assertThat(capturedIndexSetConfig.description()).isEqualTo("The Graylog default index set");
        assertThat(capturedIndexSetConfig.indexPrefix()).isEqualTo(elasticsearchConfiguration.getIndexPrefix());
        assertThat(capturedIndexSetConfig.shards()).isEqualTo(elasticsearchConfiguration.getShards());
        assertThat(capturedIndexSetConfig.replicas()).isEqualTo(elasticsearchConfiguration.getReplicas());
        assertThat(capturedIndexSetConfig.rotationStrategy()).isInstanceOf(StubRotationStrategyConfig.class);
        assertThat(capturedIndexSetConfig.retentionStrategy()).isInstanceOf(StubRetentionStrategyConfig.class);
        assertThat(capturedIndexSetConfig.indexAnalyzer()).isEqualTo(elasticsearchConfiguration.getAnalyzer());
        assertThat(capturedIndexSetConfig.indexTemplateName()).isEqualTo(elasticsearchConfiguration.getTemplateName());
        assertThat(capturedIndexSetConfig.indexOptimizationMaxNumSegments()).isEqualTo(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments());
        assertThat(capturedIndexSetConfig.indexOptimizationDisabled()).isEqualTo(elasticsearchConfiguration.isDisableIndexOptimization());
    }

    @Test
    public void upgradeThrowsIllegalStateExceptionIfIndexManagementConfigIsMissing() throws Exception {
        when(clusterConfigService.get(IndexManagementConfig.class)).thenReturn(null);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Couldn't find index management configuration");

        migration.upgrade();
    }

    @Test
    public void upgradeThrowsIllegalStateExceptionIfRotationStrategyIsMissing() throws Exception {
        when(clusterConfigService.get(IndexManagementConfig.class)).thenReturn(IndexManagementConfig.create("foobar", "test"));

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Couldn't retrieve rotation strategy provider for <foobar>");

        migration.upgrade();
    }

    @Test
    public void upgradeThrowsIllegalStateExceptionIfRetentionStrategyIsMissing() throws Exception {
        when(clusterConfigService.get(StubRotationStrategyConfig.class)).thenReturn(new StubRotationStrategyConfig());
        when(clusterConfigService.get(IndexManagementConfig.class)).thenReturn(IndexManagementConfig.create("test", "foobar"));

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Couldn't retrieve retention strategy provider for <foobar>");

        migration.upgrade();
    }

    @Test
    public void migrationDoesNotRunAgainIfMigrationWasSuccessfulBefore() throws Exception {
        when(clusterConfigService.get(DefaultIndexSetCreated.class)).thenReturn(DefaultIndexSetCreated.create());
        migration.upgrade();

        verify(clusterConfigService).get(DefaultIndexSetCreated.class);
        verifyNoMoreInteractions(clusterConfigService);
        verifyZeroInteractions(indexSetService);
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
