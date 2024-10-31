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
import org.graylog2.indexer.indexset.DefaultIndexSetConfig;
import org.graylog2.indexer.indexset.DefaultIndexSetCreated;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetConfigFactory;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.time.ZonedDateTime;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Migration creating the default index set from the legacy settings.
 */
public class V20161116172100_DefaultIndexSetMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20161116172100_DefaultIndexSetMigration.class);

    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final Map<String, Provider<RotationStrategy>> rotationStrategies;
    private final Map<String, Provider<RetentionStrategy>> retentionStrategies;
    private final IndexSetService indexSetService;
    private final ClusterConfigService clusterConfigService;
    private final IndexSetConfigFactory indexSetConfigFactory;

    @Inject
    public V20161116172100_DefaultIndexSetMigration(final ElasticsearchConfiguration elasticsearchConfiguration,
                                                    final Map<String, Provider<RotationStrategy>> rotationStrategies,
                                                    final Map<String, Provider<RetentionStrategy>> retentionStrategies,
                                                    final IndexSetService indexSetService,
                                                    final ClusterConfigService clusterConfigService,
                                                    IndexSetConfigFactory indexSetConfigFactory) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.rotationStrategies = requireNonNull(rotationStrategies);
        this.retentionStrategies = requireNonNull(retentionStrategies);
        this.indexSetService = indexSetService;
        this.clusterConfigService = clusterConfigService;
        this.indexSetConfigFactory = indexSetConfigFactory;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2016-11-16T17:21:00Z");
    }

    @Override
    public void upgrade() {
        // Do not run again if the migration marker can be found in the database.
        if (clusterConfigService.get(DefaultIndexSetCreated.class) != null) {
            return;
        }

        final IndexSetConfig config = indexSetConfigFactory.createDefault()
                .title("Default index set")
                .description("The Graylog default index set")
                .isRegular(true)
                .indexPrefix(elasticsearchConfiguration.getDefaultIndexPrefix())
                .indexTemplateName(elasticsearchConfiguration.getDefaultIndexTemplateName())
                .build();

        final IndexSetConfig savedConfig = indexSetService.save(config);
        clusterConfigService.write(DefaultIndexSetConfig.create(savedConfig.id()));
        clusterConfigService.write(DefaultIndexSetCreated.create());

        LOG.debug("Successfully created default index set: {}", savedConfig);
    }

}
