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
import org.graylog2.indexer.indexset.DefaultIndexSetCreated;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.V20161216123500_Succeeded;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * Migration for moving indexing settings into existing index sets.
 */
public class V20161216123500_DefaultIndexSetMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20161216123500_DefaultIndexSetMigration.class);

    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final IndexSetService indexSetService;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20161216123500_DefaultIndexSetMigration(final ElasticsearchConfiguration elasticsearchConfiguration,
                                                    final IndexSetService indexSetService,
                                                    final ClusterConfigService clusterConfigService) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.indexSetService = indexSetService;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.of(2016, 12, 16, 12, 35, 0, 0, ZoneOffset.UTC);
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(V20161216123500_Succeeded.class) != null) {
            return;
        }

        // The default index set must have been created first.
        checkState(clusterConfigService.get(DefaultIndexSetCreated.class) != null, "The default index set hasn't been created yet. This is a bug!");

        final IndexSetConfig defaultIndexSet= indexSetService.getDefault();
        migrateIndexSet(defaultIndexSet, elasticsearchConfiguration.getTemplateName());

        final List<IndexSetConfig> allWithoutDefault = indexSetService.findAll()
                .stream()
                .filter(indexSetConfig -> !indexSetConfig.equals(defaultIndexSet))
                .collect(Collectors.toList());

        for (IndexSetConfig indexSetConfig : allWithoutDefault) {
            migrateIndexSet(indexSetConfig, indexSetConfig.indexPrefix() + "-template");
        }


        clusterConfigService.write(V20161216123500_Succeeded.create());
    }

    private void migrateIndexSet(IndexSetConfig indexSetConfig, String templateName) {
        final String analyzer = elasticsearchConfiguration.getAnalyzer();
        final IndexSetConfig updatedConfig = indexSetConfig.toBuilder()
                .indexAnalyzer(analyzer)
                .indexTemplateName(templateName)
                .indexOptimizationMaxNumSegments(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(elasticsearchConfiguration.isDisableIndexOptimization())
                .build();

        final IndexSetConfig savedConfig = indexSetService.save(updatedConfig);

        LOG.debug("Successfully updated index set: {}", savedConfig);
    }
}
