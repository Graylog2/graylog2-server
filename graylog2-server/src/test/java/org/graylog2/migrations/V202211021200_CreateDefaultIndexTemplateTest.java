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
import org.graylog2.configuration.IndexSetDefaultTemplateConfigFactory;
import org.graylog2.configuration.IndexSetsDefaultConfiguration;
import org.graylog2.datatiering.hotonly.HotOnlyDataTieringConfig;
import org.graylog2.indexer.indexset.template.IndexSetDefaultTemplate;
import org.graylog2.indexer.indexset.template.IndexSetDefaultTemplateService;
import org.graylog2.indexer.indexset.template.IndexSetTemplate;
import org.graylog2.indexer.indexset.template.IndexSetTemplateConfig;
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
import static org.graylog2.migrations.V202211021200_CreateDefaultIndexTemplate.TEMPLATE_DESCRIPTION;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V202211021200_CreateDefaultIndexTemplateTest {

    private final static String CONFIG = """
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
    public static final String CONFIG_WITHOUT_DATA_TIERING = CONFIG.formatted("");
    private final static String DATA_TIERING_CONFIG = """
            "data_tiering": {
                  "type": "hot_only",
                  "index_lifetime_min": "P30D",
                  "index_lifetime_max": "P40D"
                },
            """;
    public static final String CONFIG_WITH_DATA_TIERING = CONFIG.formatted(DATA_TIERING_CONFIG);
    public static final String CONFIG_USE_LEGACY_FALSE = CONFIG.formatted(DATA_TIERING_CONFIG +
            """
                    "use_legacy_rotation": false,""");

    final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    @Mock
    private ElasticsearchConfiguration elasticsearchConfiguration;
    @Mock
    private ClusterConfigService clusterConfigService;
    @Mock
    private IndexSetDefaultTemplateService indexSetDefaultTemplateService;

    private IndexSetDefaultTemplateConfigFactory defaultConfigurationFactory;
    private V202211021200_CreateDefaultIndexTemplate underTest;


    @BeforeEach
    void setUp() {
        defaultConfigurationFactory = new IndexSetDefaultTemplateConfigFactory(
                elasticsearchConfiguration, new MaintenanceStrategiesHelper(elasticsearchConfiguration));
        underTest = new V202211021200_CreateDefaultIndexTemplate(
                clusterConfigService,
                defaultConfigurationFactory,
                indexSetDefaultTemplateService
        );
        when(clusterConfigService.get(IndexSetDefaultTemplate.class)).thenReturn(null);
    }

    @Test
    void testNoDefaultTemplateAndLegacyConfigExists() {
        mockElasticConfig();
        IndexSetTemplateConfig defaultConfiguration = defaultConfigurationFactory.create();

        underTest.upgrade();

        verify(indexSetDefaultTemplateService).createAndSaveDefault(createTemplate(defaultConfiguration));
        assertThat(defaultConfiguration.useLegacyRotation()).isFalse();
    }

    @Test
    void testDefaultConfigWithoutDataTieringExists() throws JsonProcessingException {
        when(clusterConfigService.get(IndexSetsDefaultConfiguration.class)).thenReturn(readLegacyConfig(CONFIG_WITHOUT_DATA_TIERING));
        IndexSetTemplateConfig defaultConfiguration = readConfig(CONFIG_WITHOUT_DATA_TIERING).toBuilder()
                .dataTiering(HotOnlyDataTieringConfig.builder()
                        .indexLifetimeMin(IndexLifetimeConfig.DEFAULT_LIFETIME_MIN)
                        .indexLifetimeMax(IndexLifetimeConfig.DEFAULT_LIFETIME_MAX)
                        .build())
                .useLegacyRotation(true)
                .build();

        underTest.upgrade();

        verify(indexSetDefaultTemplateService).createAndSaveDefault(createTemplate(defaultConfiguration));
        verify(clusterConfigService).remove(IndexSetsDefaultConfiguration.class);
    }

    @Test
    void testDefaultConfigWithDataTiering() throws JsonProcessingException {
        when(clusterConfigService.get(IndexSetsDefaultConfiguration.class)).thenReturn(readLegacyConfig(CONFIG_WITH_DATA_TIERING));
        IndexSetTemplateConfig defaultConfiguration = readConfig(CONFIG_WITH_DATA_TIERING).toBuilder().useLegacyRotation(true).build();

        underTest.upgrade();

        verify(indexSetDefaultTemplateService).createAndSaveDefault(createTemplate(defaultConfiguration));
        verify(clusterConfigService).remove(IndexSetsDefaultConfiguration.class);
    }

    @Test
    void testDefaultConfigWithDataTieringAndUseLegacyRotation() throws JsonProcessingException {
        when(clusterConfigService.get(IndexSetsDefaultConfiguration.class)).thenReturn(readLegacyConfig(CONFIG_USE_LEGACY_FALSE));
        IndexSetTemplateConfig defaultConfiguration = readConfig(CONFIG_USE_LEGACY_FALSE);

        underTest.upgrade();

        verify(indexSetDefaultTemplateService).createAndSaveDefault(createTemplate(defaultConfiguration));
        verify(clusterConfigService).remove(IndexSetsDefaultConfiguration.class);
    }

    private static IndexSetTemplate createTemplate(IndexSetTemplateConfig defaultConfiguration) {
        return new IndexSetTemplate(null, "Default Template", TEMPLATE_DESCRIPTION, false, defaultConfiguration);
    }

    private IndexSetsDefaultConfiguration readLegacyConfig(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, IndexSetsDefaultConfiguration.class);
    }

    private IndexSetTemplateConfig readConfig(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, IndexSetTemplateConfig.class);
    }

    private void mockElasticConfig() {
        when(elasticsearchConfiguration.getRotationStrategy()).thenReturn(SizeBasedRotationStrategy.NAME);
        when(elasticsearchConfiguration.getRetentionStrategy()).thenReturn(DeletionRetentionStrategy.NAME);
        when(elasticsearchConfiguration.getAnalyzer()).thenReturn("analyzer");
    }
}
