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
package org.graylog2.configuration;

import org.graylog2.plugin.cluster.ClusterConfigService;

import javax.inject.Inject;
import javax.inject.Named;

public class IndexSetsDefaultsConfigurationProvider {
    private final ClusterConfigService clusterConfigService;
    private final int serverConfigShards;
    private final int serverConfigReplicas;

    @Inject
    public IndexSetsDefaultsConfigurationProvider(ClusterConfigService clusterConfigService,
                                                  @Named("elasticsearch_shards") int serverConfigShards,
                                                  @Named("elasticsearch_replicas") int serverConfigReplicas) {
        this.clusterConfigService = clusterConfigService;
        this.serverConfigShards = serverConfigShards;
        this.serverConfigReplicas = serverConfigReplicas;
    }

    private IndexSetsDefaultsConfiguration loadFromDatabase() {
        try {
            return clusterConfigService.get(IndexSetsDefaultsConfiguration.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch index configuration from database.");
        }
    }

    public int getShards() {
        final IndexSetsDefaultsConfiguration databaseConfig = loadFromDatabase();
        // If DB config for either value is not specified, fall back to the server config defaults.
        if (databaseConfig.shards() == null) {
            return serverConfigShards;
        }
        return databaseConfig.shards();
    }

    public int getReplicas() {
        final IndexSetsDefaultsConfiguration databaseConfig = loadFromDatabase();
        // If DB config for either value is not specified, fall back to the server config defaults.
        if (databaseConfig.replicas() == null) {
            return serverConfigReplicas;
        }
        return databaseConfig.replicas();
    }
}
