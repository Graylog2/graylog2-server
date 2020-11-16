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
package org.graylog2.lookup.caches;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;
import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.checkerframework.checker.nullness.qual.NonNull;
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
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CaffeineLookupCache extends LookupCache {
    private static final Logger LOG = LoggerFactory.getLogger(CaffeineLookupCache.class);

    // Use the old GuavaLookupCache name, so we don't have to deal with migrations
    public static final String NAME = "guava_cache";
    private final Cache<LookupCacheKey, LookupResult> cache;

    @Inject
    public CaffeineLookupCache(@Assisted("id") String id,
                               @Assisted("name") String name,
                               @Assisted LookupCacheConfiguration c,
                               @Named("processbuffer_processors") int processorCount,
                               MetricRegistry metricRegistry) {
        super(id, name, c, metricRegistry);
        Config config = (Config) c;
        Caffeine<Object, Object> builder = Caffeine.newBuilder();

        builder.recordStats(() -> new MetricStatsCounter(this));

        builder.maximumSize(config.maxSize());
        builder.expireAfter(buildExpiry(config));

        cache = builder.build();
    }

    private Expiry<LookupCacheKey, LookupResult> buildExpiry(Config config) {
       return new Expiry<LookupCacheKey, LookupResult>() {
           @Override
           public long expireAfterCreate(@NonNull LookupCacheKey lookupCacheKey, @NonNull LookupResult lookupResult, long currentTime) {
               if (lookupResult.hasTTL()) {
                   return TimeUnit.MILLISECONDS.toNanos(lookupResult.cacheTTL());
               } else {
                   if (config.expireAfterWrite() > 0 && config.expireAfterWriteUnit() != null) {
                       //noinspection ConstantConditions
                       return config.expireAfterWriteUnit().toNanos(config.expireAfterWrite());
                   }
                   return Long.MAX_VALUE;
               }
           }
           @Override
           public long expireAfterUpdate(@NonNull LookupCacheKey lookupCacheKey, @NonNull LookupResult lookupResult, long currentTime, long currentDuration) {
               return currentDuration;
           }

           @Override
           public long expireAfterRead(@NonNull LookupCacheKey lookupCacheKey, @NonNull LookupResult lookupResult, long currentTime, long currentDuration) {
               if (config.expireAfterAccess() > 0 && config.expireAfterAccessUnit() != null) {
                   //noinspection ConstantConditions
                   return config.expireAfterAccessUnit().toNanos(config.expireAfterAccess());
               }
               return currentDuration;
           }
       };
    }

    @Override
    public long entryCount() {
        if (cache != null) {
            return cache.estimatedSize();
        } else {
            return 0L;
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
        final Function<LookupCacheKey, LookupResult> mapFunction = unused -> {
            try {
                return loader.call();
            } catch (Exception e) {
                LOG.warn("Loading value from data adapter failed for key {}, returning empty result", key, e);
                return LookupResult.withError();
            }
        };
        try (final Timer.Context ignored = lookupTimer()) {
            return cache.get(key, mapFunction);
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
        CaffeineLookupCache create(@Assisted("id") String id, @Assisted("name") String name, LookupCacheConfiguration configuration);

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends LookupCache.Descriptor<CaffeineLookupCache.Config> {
        public Descriptor() {
            super(NAME, CaffeineLookupCache.Config.class);
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
    @JsonDeserialize(builder = AutoValue_CaffeineLookupCache_Config.Builder.class)
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
            return new AutoValue_CaffeineLookupCache_Config.Builder();
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

    private static class MetricStatsCounter implements StatsCounter {
        private final LookupCache cache;

        MetricStatsCounter(LookupCache cache) {
            this.cache = cache;
        }

        @Override
        public void recordHits(int count) {
            cache.incrHitCount(count);
            cache.incrTotalCount(count);
        }

        @Override
        public void recordMisses(int count) {
            cache.incrMissCount(count);
            cache.incrTotalCount(count);
        }

        @Override
        public void recordLoadSuccess(long loadTime) {}

        @Override
        public void recordLoadFailure(long loadTime) {}

        @Override
        public void recordEviction() {}

        @Override
        public @NonNull CacheStats snapshot() {
            throw new UnsupportedOperationException("snapshots not implemented");
        }
    }
}
