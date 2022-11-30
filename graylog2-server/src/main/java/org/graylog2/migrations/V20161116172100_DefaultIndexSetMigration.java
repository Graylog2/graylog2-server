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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

/**
 * Migration creating the default index set from the legacy settings.
 */
public class V20161116172100_DefaultIndexSetMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20161116172100_DefaultIndexSetMigration.class);

    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final IndexSetService indexSetService;
    private final IndexSetConfigFactory indexSetConfigFactory;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20161116172100_DefaultIndexSetMigration(final ElasticsearchConfiguration elasticsearchConfiguration,
                                                    final IndexSetService indexSetService,
                                                    final ClusterConfigService clusterConfigService,
                                                    final IndexSetConfigFactory indexSetConfigFactory) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.indexSetService = indexSetService;
        this.indexSetConfigFactory = indexSetConfigFactory;
        this.clusterConfigService = clusterConfigService;
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
