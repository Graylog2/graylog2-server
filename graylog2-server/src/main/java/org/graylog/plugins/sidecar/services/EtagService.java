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
package org.graylog.plugins.sidecar.services;

import com.codahale.metrics.MetricRegistry;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog.plugins.sidecar.common.SidecarPluginConfiguration;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.metrics.CacheStatsSet;
import org.graylog2.shared.metrics.MetricUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.codahale.metrics.MetricRegistry.name;

@Singleton
public class EtagService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(EtagService.class);

    private final Cache<String, Boolean> cache;
    private MetricRegistry metricRegistry;
    private EventBus eventBus;
    private ClusterEventBus clusterEventBus;

    @Inject
    public EtagService(SidecarPluginConfiguration pluginConfiguration,
                       MetricRegistry metricRegistry,
                       EventBus eventBus,
                       ClusterEventBus clusterEventBus) {
        this.metricRegistry = metricRegistry;
        this.eventBus = eventBus;
        this.clusterEventBus = clusterEventBus;
        Duration cacheTime = pluginConfiguration.getCacheTime();
        cache = CacheBuilder.newBuilder()
                .recordStats()
                .expireAfterWrite(cacheTime.getQuantity(), cacheTime.getUnit())
                .maximumSize(pluginConfiguration.getCacheMaxSize())
                .build();
    }

    @Subscribe
    public void handleEtagInvalidation(EtagCacheInvalidation event) {
        if (event.etag().equals("")) {
            LOG.trace("Invalidating all collector configuration etags");
            cache.invalidateAll();
        } else {
            LOG.trace("Invalidating collector configuration etag {}", event.etag());
            cache.invalidate(event.etag());
        }
    }

    public boolean isPresent(String etag) {
        return cache.getIfPresent(etag) != null;
    }

    public void put(String etag) {
        cache.put(etag, Boolean.TRUE);
    }

    public void invalidate(String etag) {
        clusterEventBus.post(EtagCacheInvalidation.etag(etag));
    }

    public void invalidateAll() {
        cache.invalidateAll();
        clusterEventBus.post(EtagCacheInvalidation.etag(""));
    }

    @Override
    protected void startUp() throws Exception {
        eventBus.register(this);
        MetricUtils.safelyRegisterAll(metricRegistry, new CacheStatsSet(name(ConfigurationService.class, "etag-cache"), cache));
    }

    @Override
    protected void shutDown() throws Exception {
        eventBus.unregister(this);
        metricRegistry.removeMatching((name, metric) -> name.startsWith(name(ConfigurationService.class, "etag-cache")));
    }
}
