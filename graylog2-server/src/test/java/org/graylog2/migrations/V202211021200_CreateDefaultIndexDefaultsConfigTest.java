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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.configuration.IndexSetsDefaultConfiguration;
import org.graylog2.configuration.IndexSetsDefaultConfigurationFactory;
import org.graylog2.datatiering.hotonly.HotOnlyDataTieringConfig;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategy;
import org.graylog2.indexer.rotation.tso.IndexLifetimeConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V202211021200_CreateDefaultIndexDefaultsConfigTest {

    private final static String DEFAULT_CONFIG = """
            {
                "index_analyzer": "standard",
                "shards": 4,
                "replicas": 0,
                "index_optimization_max_num_segments": 1,
                "index_optimization_disabled": false,
                "field_type_refresh_interval": 5,
                "field_type_refresh_interval_unit": "SECONDS",
                "rotation_strategy_class": "org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy",
                "rotation_strategy_config": {
                  "type": "org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig",
                  "max_docs_per_index": 10000000
                },
                "retention_strategy_class": "org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy",
                "retention_strategy_config": {
                  "type": "org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig",
                  "max_number_of_indices": 5
                },
                %s
                "retention_strategy": {
                  "type": "org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig",
                  "max_number_of_indices": 5
                },
                "rotation_strategy": {
                  "type": "org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig",
                  "max_docs_per_index": 10000000
                }
              }
            """;
    private final static String DATA_TIERING_CONFIG = """
            "data_tiering": {
                  "type": "hot_only",
                  "index_lifetime_min": "P30D",
                  "index_lifetime_max": "P40D"
                },
            """;
    final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    @Mock
    private ElasticsearchConfiguration elasticsearchConfiguration;
    @Mock
    private ClusterConfigService clusterConfigService;

    private IndexSetsDefaultConfigurationFactory defaultConfigurationFactory;
    private V202211021200_CreateDefaultIndexDefaultsConfig underTest;


    @BeforeEach
    void setUp() {
        defaultConfigurationFactory = new IndexSetsDefaultConfigurationFactory(
                elasticsearchConfiguration, new MaintenanceStrategiesHelper(elasticsearchConfiguration));
        underTest = new V202211021200_CreateDefaultIndexDefaultsConfig(
                clusterConfigService,
                defaultConfigurationFactory);
    }

    @Test
    void testNoDefaultConfigurationExists() {
        mockElasticConfig();

        underTest.upgrade();
        IndexSetsDefaultConfiguration defaultConfiguration = defaultConfigurationFactory.create();

        verify(clusterConfigService).write(defaultConfiguration);
        assertThat(defaultConfiguration.useLegacyRotation()).isFalse();
    }

    @Test
    void testDefaultConfigWithoutDataTiering() throws JsonProcessingException {
        IndexSetsDefaultConfiguration defaultConfiguration = readConfig(DEFAULT_CONFIG.formatted(""));
        when(clusterConfigService.get(IndexSetsDefaultConfiguration.class)).thenReturn(defaultConfiguration);

        underTest.upgrade();

        IndexSetsDefaultConfiguration expected = defaultConfiguration.toBuilder()
                .dataTiering(HotOnlyDataTieringConfig.builder()
                        .indexLifetimeMin(IndexLifetimeConfig.DEFAULT_LIFETIME_MIN)
                        .indexLifetimeMax(IndexLifetimeConfig.DEFAULT_LIFETIME_MAX)
                        .build())
                .build();
        verify(clusterConfigService).write(expected);
        assertThat(expected.useLegacyRotation()).isTrue();
    }

    @Test
    void testDefaultConfigWithDataTiering() throws JsonProcessingException {
        IndexSetsDefaultConfiguration defaultConfiguration = readConfig(DEFAULT_CONFIG.formatted(DATA_TIERING_CONFIG));
        when(clusterConfigService.get(IndexSetsDefaultConfiguration.class)).thenReturn(defaultConfiguration);

        underTest.upgrade();

        verify(clusterConfigService, never()).write(any());
        assertThat(defaultConfiguration.useLegacyRotation()).isTrue();
    }

    @Test
    void testDefaultConfigWithDataTieringAndUseLegacyRotation() throws JsonProcessingException {
        IndexSetsDefaultConfiguration defaultConfiguration = readConfig(DEFAULT_CONFIG.formatted(DATA_TIERING_CONFIG +
                """
                        "use_legacy_rotation": false,"""));
        when(clusterConfigService.get(IndexSetsDefaultConfiguration.class)).thenReturn(defaultConfiguration);

        underTest.upgrade();

        verify(clusterConfigService, never()).write(any());
        assertThat(defaultConfiguration.useLegacyRotation()).isFalse();
    }

    private IndexSetsDefaultConfiguration readConfig(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, IndexSetsDefaultConfiguration.class);
    }

    private void mockElasticConfig() {
        when(elasticsearchConfiguration.getRotationStrategy()).thenReturn(SizeBasedRotationStrategy.NAME);
        when(elasticsearchConfiguration.getRetentionStrategy()).thenReturn(DeletionRetentionStrategy.NAME);
        when(elasticsearchConfiguration.getAnalyzer()).thenReturn("analyzer");
    }
}
