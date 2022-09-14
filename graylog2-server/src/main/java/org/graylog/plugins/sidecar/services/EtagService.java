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
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    private final Cache<String, Boolean> collectorCache;
    private final Cache<String, Boolean> configurationCache;
    private final Cache<String, String> assignmentsCache;
    private final MetricRegistry metricRegistry;
    private final EventBus eventBus;
    private final ClusterEventBus clusterEventBus;

    @JsonAutoDetect
    enum CacheContext {
        @JsonProperty("collector")
        COLLECTOR,
        @JsonProperty("configuration")
        CONFIGURATION,
        @JsonProperty("assignments")
        ASSIGNMENTS
    }

    @Inject
    public EtagService(SidecarPluginConfiguration pluginConfiguration,
                       MetricRegistry metricRegistry,
                       EventBus eventBus,
                       ClusterEventBus clusterEventBus) {
        this.metricRegistry = metricRegistry;
        this.eventBus = eventBus;
        this.clusterEventBus = clusterEventBus;
        Duration cacheTime = pluginConfiguration.getCacheTime();
        collectorCache = CacheBuilder.newBuilder()
                .recordStats()
                .expireAfterWrite(cacheTime.getQuantity(), cacheTime.getUnit())
                .maximumSize(pluginConfiguration.getCacheMaxSize())
                .build();

        configurationCache = CacheBuilder.newBuilder()
                .recordStats()
                .expireAfterWrite(cacheTime.getQuantity(), cacheTime.getUnit())
                .maximumSize(pluginConfiguration.getCacheMaxSize())
                .build();

        assignmentsCache = CacheBuilder.newBuilder()
                .recordStats()
                .expireAfterWrite(cacheTime.getQuantity(), cacheTime.getUnit())
                .maximumSize(pluginConfiguration.getCacheMaxSize())
                .build();
    }

    @Subscribe
    public void handleEtagInvalidation(EtagCacheInvalidation event) {
        //noinspection rawtypes
        Cache cache;
        switch (event.cacheContext()) {
            case COLLECTOR -> cache = collectorCache;
            case CONFIGURATION -> cache = configurationCache;
            case ASSIGNMENTS -> cache = assignmentsCache;
            default -> throw new IllegalArgumentException("Unknown cache context");
        }

        if (event.cacheKey().equals("")) {
            LOG.trace("Invalidating {} cache for all keys", event.cacheContext());
            cache.invalidateAll();
        } else {
            LOG.trace("Invalidating {} cache for cacheKey {}", event.cacheContext(), event.cacheKey());
            cache.invalidate(event.cacheKey());
        }
    }

    public boolean collectorsAreCached(String etag) {
        return collectorCache.getIfPresent(etag) != null;
    }

    public boolean configurationsAreCached(String etag) {
        return configurationCache.getIfPresent(etag) != null;
    }

    public boolean assignmentsAreCached(String sidecarId, String etag) {
        return etag.equals(assignmentsCache.getIfPresent(sidecarId));
    }

    public void registerCollector(String etag) {
        collectorCache.put(etag, Boolean.TRUE);
    }

    public void registerConfiguration(String etag) {
        configurationCache.put(etag, Boolean.TRUE);
    }

    public void registerAssignment(String sidecarId, String etag) {
        assignmentsCache.put(sidecarId, etag);
    }


    public void invalidateAllConfigurations() {
        configurationCache.invalidateAll();
        clusterEventBus.post(EtagCacheInvalidation.create(CacheContext.CONFIGURATION, ""));
    }

    public void invalidateAllCollectors() {
        collectorCache.invalidateAll();
        clusterEventBus.post(EtagCacheInvalidation.create(CacheContext.COLLECTOR, ""));
    }

    public void invalidateAllAssignments() {
        assignmentsCache.invalidateAll();
        clusterEventBus.post(EtagCacheInvalidation.create(CacheContext.ASSIGNMENTS, ""));
    }

    public void invalidateAssignment(String sidecarId) {
        assignmentsCache.invalidate(sidecarId);
        clusterEventBus.post(EtagCacheInvalidation.create(CacheContext.ASSIGNMENTS, sidecarId));
    }

    @Override
    protected void startUp() throws Exception {
        eventBus.register(this);
        MetricUtils.safelyRegisterAll(metricRegistry, new CacheStatsSet(name(ConfigurationService.class, "etag-cache"), configurationCache));
        MetricUtils.safelyRegisterAll(metricRegistry, new CacheStatsSet(name(CollectorService.class, "etag-cache"), collectorCache));
        MetricUtils.safelyRegisterAll(metricRegistry, new CacheStatsSet(name(SidecarService.class, "etag-cache"), assignmentsCache));
    }

    @Override
    protected void shutDown() throws Exception {
        eventBus.unregister(this);
        metricRegistry.removeMatching((name, metric) -> name.startsWith(name(ConfigurationService.class, "etag-cache")));
        metricRegistry.removeMatching((name, metric) -> name.startsWith(name(CollectorService.class, "etag-cache")));
        metricRegistry.removeMatching((name, metric) -> name.startsWith(name(SidecarService.class, "etag-cache")));
    }
}
