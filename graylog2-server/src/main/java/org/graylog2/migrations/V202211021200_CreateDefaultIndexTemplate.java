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

import jakarta.inject.Inject;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.configuration.IndexSetDefaultTemplateConfigFactory;
import org.graylog2.configuration.IndexSetsDefaultConfiguration;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.datatiering.fallback.PlaceholderDataTieringConfig;
import org.graylog2.indexer.indexset.template.IndexSetDefaultTemplateService;
import org.graylog2.indexer.indexset.template.IndexSetTemplate;
import org.graylog2.indexer.indexset.template.IndexSetTemplateConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Create initial index set default configuration based on {@link ElasticsearchConfiguration} values.
 */
public class V202211021200_CreateDefaultIndexTemplate extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V202211021200_CreateDefaultIndexTemplate.class);
    public static final String TEMPLATE_DESCRIPTION = "Generated template that is used for index sets which are created automatically.";

    private final ClusterConfigService clusterConfigService;
    private final IndexSetDefaultTemplateConfigFactory factory;
    private final IndexSetDefaultTemplateService indexSetDefaultTemplateService;

    @Inject
    public V202211021200_CreateDefaultIndexTemplate(final ClusterConfigService clusterConfigService,
                                                    IndexSetDefaultTemplateConfigFactory factory,
                                                    IndexSetDefaultTemplateService indexSetDefaultTemplateService) {
        this.clusterConfigService = clusterConfigService;
        this.factory = factory;
        this.indexSetDefaultTemplateService = indexSetDefaultTemplateService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2022-11-02T12:00:00Z");
    }

    @Override
    public void upgrade() {
        Optional<IndexSetTemplate> defaultIndexSetTemplate = indexSetDefaultTemplateService.getDefaultIndexSetTemplate();
        if (defaultIndexSetTemplate.isEmpty()) {
            IndexSetsDefaultConfiguration legacyDefaultConfig = clusterConfigService.get(IndexSetsDefaultConfiguration.class);
            if (legacyDefaultConfig == null) {
                saveDefaultTemplate(factory.create());
            } else {
                saveDefaultTemplate(createTemplateConfig(legacyDefaultConfig));
                removeLegacyConfig();
            }
        } else {
            LOG.debug("Migration already completed.");
        }
    }

    private IndexSetTemplateConfig createTemplateConfig(IndexSetsDefaultConfiguration legacyDefaultConfig) {
        DataTieringConfig dataTieringConfig = legacyDefaultConfig.dataTiering();
        if (legacyDefaultConfig.dataTiering() instanceof PlaceholderDataTieringConfig) {
            dataTieringConfig = factory.getDataTieringConfig(legacyDefaultConfig.rotationStrategyConfig());
        }

        return IndexSetTemplateConfig.builder()
                .indexAnalyzer(legacyDefaultConfig.indexAnalyzer())
                .shards(legacyDefaultConfig.shards())
                .replicas(legacyDefaultConfig.replicas())
                .indexOptimizationMaxNumSegments(legacyDefaultConfig.indexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(legacyDefaultConfig.indexOptimizationDisabled())
                .fieldTypeRefreshInterval(Duration.standardSeconds(
                        legacyDefaultConfig.fieldTypeRefreshIntervalUnit().toSeconds(legacyDefaultConfig.fieldTypeRefreshInterval())))
                .rotationStrategyClass(legacyDefaultConfig.rotationStrategyClass())
                .rotationStrategy(legacyDefaultConfig.rotationStrategyConfig())
                .retentionStrategyClass(legacyDefaultConfig.retentionStrategyClass())
                .retentionStrategy(legacyDefaultConfig.retentionStrategyConfig())
                .dataTiering(dataTieringConfig)
                .useLegacyRotation(legacyDefaultConfig.useLegacyRotation())
                .build();
    }

    private void saveDefaultTemplate(IndexSetTemplateConfig indexSetTemplateConfig) {
        IndexSetTemplate defaultIndexSetTemplate = new IndexSetTemplate(
                null, "Default Template", TEMPLATE_DESCRIPTION, false, indexSetTemplateConfig
        );
        try {
            indexSetDefaultTemplateService.createAndSaveDefault(defaultIndexSetTemplate);
            LOG.debug("IndexSetDefaultTemplate saved.");
        } catch (Exception e) {
            LOG.error("Unable to write IndexSetDefaultTemplate.", e);
        }
    }

    private void removeLegacyConfig() {
        try {
            clusterConfigService.remove(IndexSetsDefaultConfiguration.class);
        } catch (Exception e) {
            LOG.debug("Unable to remove legacy IndexSetsDefaultConfiguration configuration.", e);
        }
    }
}
