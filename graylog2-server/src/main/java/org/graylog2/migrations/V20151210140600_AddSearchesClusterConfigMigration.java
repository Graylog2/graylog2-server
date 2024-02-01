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
import org.graylog2.indexer.searches.SearchesClusterConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;

public class V20151210140600_AddSearchesClusterConfigMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20151210140600_AddSearchesClusterConfigMigration.class);

    private final ClusterConfigService clusterConfigService;
    private final ElasticsearchConfiguration elasticsearchConfiguration;

    @Inject
    public V20151210140600_AddSearchesClusterConfigMigration(ClusterConfigService clusterConfigService, ElasticsearchConfiguration elasticsearchConfiguration) {
        this.clusterConfigService = clusterConfigService;
        this.elasticsearchConfiguration = elasticsearchConfiguration;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2015-12-10T14:06:00Z");
    }

    @Override
    public void upgrade() {
        final SearchesClusterConfig searchesClusterConfig = clusterConfigService.get(SearchesClusterConfig.class);
        if (searchesClusterConfig == null) {
            final SearchesClusterConfig config = SearchesClusterConfig.createDefault();
            LOG.info("Creating searches cluster config: {}", config);
            clusterConfigService.write(config);
        }
    }
}
