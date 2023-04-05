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
package org.graylog2.security;

import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.CacheBuilder;
import com.google.common.primitives.Ints;
import com.google.inject.Singleton;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.CacheManager;
import org.graylog2.metrics.CacheStatsSet;
import org.graylog2.shared.metrics.MetricUtils;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Singleton
public class MongoDbAuthorizationCacheManager implements CacheManager {
    private final MetricRegistry registry;

    @Inject
    public MongoDbAuthorizationCacheManager(MetricRegistry registry) {
        this.registry = registry;
    }

    @Override
    public <K, V> Cache<K, V> getCache(String name) throws CacheException {
        return new GuavaCacheWrapper<>(registry);
    }

    private static class GuavaCacheWrapper<K, V> implements Cache<K, V> {

        private com.google.common.cache.Cache<K, V> cache;

        public GuavaCacheWrapper(MetricRegistry registry) {
            cache = CacheBuilder.newBuilder()
                    .expireAfterWrite(1, TimeUnit.SECONDS)
                    .maximumSize(1000)
                    .concurrencyLevel(8)
                    .recordStats()
                    .build();
            MetricUtils.safelyRegisterAll(registry, new CacheStatsSet(MetricRegistry.name(MongoDbAuthorizationCacheManager.class, "cache"), cache));
        }

        @Override
        public V get(K key) throws CacheException {
            return cache.getIfPresent(key);
        }

        @Override
        public V put(K key, V value) throws CacheException {
            final V old = cache.getIfPresent(key);
            cache.put(key, value);
            return old;
        }

        @Override
        public V remove(K key) throws CacheException {
            final V old = cache.getIfPresent(key);
            cache.invalidate(key);
            return old;
        }

        @Override
        public void clear() throws CacheException {
            cache.invalidateAll();
        }

        @Override
        public int size() {
            return Ints.saturatedCast(cache.size());
        }

        @Override
        public Set<K> keys() {
            return cache.asMap().keySet();
        }

        @Override
        public Collection<V> values() {
            return cache.asMap().values();
        }
    }
}
