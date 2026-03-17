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
package org.graylog.collectors;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.util.Optional;

/**
 * Provides access to the collector configuration.
 */
@Singleton
public class CollectorsConfigService {
    private final ClusterConfigService clusterConfigService;

    @Inject
    public CollectorsConfigService(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    /**
     * Get the current config.
     *
     * @return the current config or an empty optional
     */
    public Optional<CollectorsConfig> get() {
        return Optional.ofNullable(clusterConfigService.get(CollectorsConfig.class));
    }

    /**
     * Get the current config or a default config.
     *
     * @return the current config or a default config
     */
    public CollectorsConfig getOrDefault() {
        return get().orElse(CollectorsConfig.createDefault("localhost"));
    }

    /**
     * Save collectors config to the database.
     *
     * @param config the config object
     */
    public void save(CollectorsConfig config) {
        clusterConfigService.write(config);
    }
}
