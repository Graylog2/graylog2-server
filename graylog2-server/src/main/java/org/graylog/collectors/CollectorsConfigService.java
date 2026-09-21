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

import com.google.common.base.Suppliers;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.collectors.events.CollectorsConfigUpdatedEvent;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Provides access to the collector configuration.
 */
@Singleton
public class CollectorsConfigService {
    private final ClusterConfigService clusterConfigService;
    private final URI httpExternalUri;
    private final EventBus eventBus;

    private final AtomicReference<Supplier<Optional<CollectorsConfig>>> config;

    @Inject
    public CollectorsConfigService(ClusterConfigService clusterConfigService,
                                   HttpConfiguration httpConfiguration,
                                   EventBus eventBus) {
        this.clusterConfigService = clusterConfigService;
        this.httpExternalUri = httpConfiguration.getHttpExternalUri();
        this.config = new AtomicReference<>(Suppliers.memoize(this::loadConfig));
        this.eventBus = eventBus;

        eventBus.register(this);
    }

    @Subscribe
    public void handleClusterConfigChangedEvent(ClusterConfigChangedEvent event) {
        if (CollectorsConfig.class.getCanonicalName().equals(event.type())) {
            invalidate();
            eventBus.post(new CollectorsConfigUpdatedEvent());
        }
    }

    /**
     * Get the current config.
     *
     * @return the current config or an empty optional
     */
    public Optional<CollectorsConfig> get() {
        return config.get().get();
    }

    /**
     * Get the current config or a default config.
     *
     * @return the current config or a default config
     */
    public CollectorsConfig getOrDefault() {
        return get().orElse(CollectorsConfig.createDefault(httpExternalUri.getHost()));
    }

    /**
     * Save collectors config to the database.
     *
     * @param config the config object
     */
    public void save(CollectorsConfig config) {
        clusterConfigService.write(config);
        invalidate(); // prevent reading a stale cache entry immediately after saving
    }

    private Optional<CollectorsConfig> loadConfig() {
        return Optional.ofNullable(clusterConfigService.get(CollectorsConfig.class));
    }

    private void invalidate() {
        config.set(Suppliers.memoize(this::loadConfig));
    }
}
