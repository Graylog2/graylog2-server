/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.lookup.caches;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;
import org.graylog2.plugin.lookup.LookupCacheKey;
import org.graylog2.plugin.lookup.LookupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.validation.constraints.Min;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GuavaLookupCache extends LookupCache {
    private static final Logger LOG = LoggerFactory.getLogger(GuavaLookupCache.class);

    public static final String NAME = "guava_cache";
    private final Cache<LookupCacheKey, LookupResult> cache;

    @Inject
    public GuavaLookupCache(@Assisted("id") String id,
                            @Assisted("name") String name,
                            @Assisted LookupCacheConfiguration c,
                            @Named("processbuffer_processors") int processorCount,
                            MetricRegistry metricRegistry) {
        super(id, name, c, metricRegistry);
        Config config = (Config) c;
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();

        // the theory is that typically only processors will affect the cache concurrency, whereas decorator usage is less critical
        builder.concurrencyLevel(processorCount)
                .recordStats();

        builder.maximumSize(config.maxSize());
        if (config.expireAfterAccess() > 0 && config.expireAfterAccessUnit() != null) {
            //noinspection ConstantConditions
            builder.expireAfterAccess(config.expireAfterAccess(), config.expireAfterAccessUnit());
        }
        if (config.expireAfterWrite() > 0 && config.expireAfterWriteUnit() != null) {
            //noinspection ConstantConditions
            builder.expireAfterWrite(config.expireAfterWrite(), config.expireAfterWriteUnit());
        }

        cache = new InstrumentedCache<>(builder.build(), this);
    }

    @Override
    public Gauge<Long> entryCount() {
        if (cache != null) {
            return cache::size;
        } else {
            return () -> 0L;
        }
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }

    @Override
    public LookupResult get(LookupCacheKey key, Callable<LookupResult> loader) {
        try (final Timer.Context ignored = lookupTimer()) {
            return cache.get(key, loader);
        } catch (ExecutionException e) {
            LOG.warn("Loading value from data adapter failed for key {}, returning empty result", key, e);
            return LookupResult.empty();
        }
    }

    @Override
    public LookupResult getIfPresent(LookupCacheKey key) {
        final LookupResult cacheEntry = cache.getIfPresent(key);
        if (cacheEntry == null) {
            return LookupResult.empty();
        }
        return cacheEntry;
    }

    @Override
    public void purge() {
        cache.invalidateAll();
    }

    @Override
    public void purge(LookupCacheKey purgeKey) {
        if (purgeKey.isPrefixOnly()) {
            // If the key to purge only contains a prefix, invalidate all keys with that prefix
            cache.invalidateAll(
                    cache.asMap().keySet().stream()
                            .filter(lookupCacheKey -> purgeKey.prefix().equals(lookupCacheKey.prefix()))
                            .collect(Collectors.toSet())
            );
        } else {
            cache.invalidate(purgeKey);
        }
    }

    public interface Factory extends LookupCache.Factory {
        @Override
        GuavaLookupCache create(@Assisted("id") String id, @Assisted("name") String name, LookupCacheConfiguration configuration);

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends LookupCache.Descriptor<GuavaLookupCache.Config> {
        public Descriptor() {
            super(NAME, GuavaLookupCache.Config.class);
        }

        @Override
        public Config defaultConfiguration() {
            return Config.builder()
                    .type(NAME)
                    .maxSize(1000)
                    .expireAfterAccess(60)
                    .expireAfterAccessUnit(TimeUnit.SECONDS)
                    .expireAfterWrite(0)
                    .build();
        }
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    @JsonDeserialize(builder = AutoValue_GuavaLookupCache_Config.Builder.class)
    @JsonTypeName(NAME)
    public abstract static class Config implements LookupCacheConfiguration {

        @Min(0)
        @JsonProperty("max_size")
        public abstract int maxSize();

        @Min(0)
        @JsonProperty("expire_after_access")
        public abstract long expireAfterAccess();

        @Nullable
        @JsonProperty("expire_after_access_unit")
        public abstract TimeUnit expireAfterAccessUnit();

        @Min(0)
        @JsonProperty("expire_after_write")
        public abstract long expireAfterWrite();

        @Nullable
        @JsonProperty("expire_after_write_unit")
        public abstract TimeUnit expireAfterWriteUnit();

        public static Builder builder() {
            return new AutoValue_GuavaLookupCache_Config.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty("type")
            public abstract Builder type(String type);

            @JsonProperty("max_size")
            public abstract Builder maxSize(int maxSize);

            @JsonProperty("expire_after_access")
            public abstract Builder expireAfterAccess(long expireAfterAccess);

            @JsonProperty("expire_after_access_unit")
            public abstract Builder expireAfterAccessUnit(@Nullable TimeUnit expireAfterAccessUnit);

            @JsonProperty("expire_after_write")
            public abstract Builder expireAfterWrite(long expireAfterWrite);

            @JsonProperty("expire_after_write_unit")
            public abstract Builder expireAfterWriteUnit(@Nullable TimeUnit expireAfterWriteUnit);

            public abstract Config build();
        }
    }

    private static class InstrumentedCache<K, V> implements Cache<K, V> {
        private final Cache<K, V> delegate;
        private final LookupCache cache;

        public InstrumentedCache(Cache<K, V> delegate, LookupCache cache) {
            this.delegate = delegate;
            this.cache = cache;
        }

        @Nullable
        @Override
        public V getIfPresent(Object key) {
            return delegate.getIfPresent(key);
        }

        @Override
        public V get(K key, Callable<? extends V> loader) throws ExecutionException {
            V value = delegate.getIfPresent(key);
            if (value == null) {
                try {
                    value = loader.call();
                    delegate.put(key, value);
                    cache.incrMissCount();
                } catch (Exception e) {
                    throw new ExecutionException(e);
                }
            } else {
                cache.incrHitCount();
            }

            cache.incrTotalCount();

            return value;
        }

        @Override
        public ImmutableMap<K, V> getAllPresent(Iterable<?> keys) {
            return delegate.getAllPresent(keys);
        }

        @Override
        public void put(K key, V value) {
            delegate.put(key, value);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            delegate.putAll(m);
        }

        @Override
        public void invalidate(Object key) {
            delegate.invalidate(key);
        }

        @Override
        public void invalidateAll(Iterable<?> keys) {
            delegate.invalidateAll(keys);
        }

        @Override
        public void invalidateAll() {
            delegate.invalidateAll();
        }

        @Override
        public long size() {
            return delegate.size();
        }

        @Override
        public CacheStats stats() {
            return delegate.stats();
        }

        @Override
        public ConcurrentMap<K, V> asMap() {
            return delegate.asMap();
        }

        @Override
        public void cleanUp() {
            delegate.cleanUp();
        }
    }
}
