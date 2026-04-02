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
import org.graylog.collectors.events.CollectorCaConfigUpdated;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.util.Objects;
import java.util.Optional;

/**
 * Provides access to the collector configuration.
 */
@Singleton
public class CollectorsConfigService {
    private static final CollectorsConfig DEFAULT_CONFIG = CollectorsConfig.createDefault("localhost");

    private final ClusterConfigService clusterConfigService;
    private final ClusterEventBus clusterEventBus;

    @Inject
    public CollectorsConfigService(ClusterConfigService clusterConfigService, ClusterEventBus clusterEventBus) {
        this.clusterConfigService = clusterConfigService;
        this.clusterEventBus = clusterEventBus;
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
     * Get the OpAMP max request body size in bytes.
     *
     * @return the max request body size
     */
    public int getOpampMaxRequestBodySizeBytes() {
        // TODO: Switch to getting the actual database value once we have caching and cache invalidation in place.
        return DEFAULT_CONFIG.opampMaxRequestBodySizeBytes();
    }

    /**
     * Save collectors config to the database.
     *
     * @param config the config object
     */
    public void save(CollectorsConfig config) {
        final var existing = get();

        clusterConfigService.write(config);


        existing.ifPresent(c -> {
            if (!Objects.equals(c.caCertId(), config.caCertId())
                    || !Objects.equals(c.signingCertId(), config.signingCertId())
                    || !Objects.equals(c.otlpServerCertId(), config.otlpServerCertId())) {
                clusterEventBus.post(new CollectorCaConfigUpdated());
            }
        });
    }
}
