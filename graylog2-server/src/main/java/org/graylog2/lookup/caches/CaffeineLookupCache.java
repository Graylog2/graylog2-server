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
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Ticker;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;
import com.google.auto.value.AutoValue;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Min;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;
import org.graylog2.plugin.lookup.LookupCacheKey;
import org.graylog2.plugin.lookup.LookupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CaffeineLookupCache extends LookupCache {
    private static final Logger LOG = LoggerFactory.getLogger(CaffeineLookupCache.class);

    // Use the old GuavaLookupCache name, so we don't have to deal with migrations
    public static final String NAME = "guava_cache";
    public static final String MAX_SIZE = "max_size";
    public static final String EXPIRE_AFTER_ACCESS = "expire_after_access";
    public static final String EXPIRE_AFTER_ACCESS_UNIT = "expire_after_access_unit";
    public static final String EXPIRE_AFTER_WRITE = "expire_after_write";
    public static final String EXPIRE_AFTER_WRITE_UNIT = "expire_after_write_unit";
    public static final String IGNORE_NULL = "ignore_null";
    public static final String TTL_EMPTY = "ttl_empty";
    public static final String TTL_EMPTY_UNIT = "ttl_empty_unit";
    private final Cache<LookupCacheKey, LookupResult> cache;
    private final Config config;

    @Inject
    public CaffeineLookupCache(@Assisted("id") String id,
                               @Assisted("name") String name,
                               @Assisted LookupCacheConfiguration c,
                               MetricRegistry metricRegistry) {
        super(id, name, c, metricRegistry);
        config = (Config) c;
        cache = Caffeine.newBuilder()
                .recordStats(() -> new MetricStatsCounter(this))
                .maximumSize(config.maxSize())
                .expireAfter(buildExpiry(config))
                .build();
    }

    // Constructor with external ticker for testing
    public CaffeineLookupCache(String id,
                               String name,
                               LookupCacheConfiguration c,
                               MetricRegistry metricRegistry,
                               Ticker fakeTicker) {
        super(id, name, c, metricRegistry);
        config = (Config) c;
        cache = Caffeine.newBuilder()
                .recordStats(() -> new MetricStatsCounter(this))
                .maximumSize(config.maxSize())
                .expireAfter(buildExpiry(config))
                .ticker(fakeTicker)
                .build();
    }

    private Expiry<LookupCacheKey, LookupResult> buildExpiry(Config config) {
        return new Expiry<>() {
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
                if (config.ttlEmpty() != null
                        && !Boolean.TRUE.equals(config.ignoreNull())
                        && lookupResult.isEmpty()) {
                    LOG.trace("afterRead: empty: {}", currentDuration);
                    return currentDuration;
                }
                if (config.expireAfterAccess() > 0 && config.expireAfterAccessUnit() != null) {
                    //noinspection ConstantConditions
                    LOG.trace("afterRead: config: {}", config.expireAfterAccessUnit().toNanos(config.expireAfterAccess()));
                    return config.expireAfterAccessUnit().toNanos(config.expireAfterAccess());
                }
                LOG.trace("afterRead: {}", currentDuration);
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
        // no action required
    }

    @Override
    protected void doStop() throws Exception {
        // no action required
    }

    @Override
    public LookupResult get(LookupCacheKey key, Callable<LookupResult> loader) {
        final Function<LookupCacheKey, LookupResult> mapFunction = unused -> {
            try {
                final LookupResult result = loader.call();
                if (ignoreResult(result, config.ignoreNull())) {
                    LOG.trace("Ignoring failed lookup for key {}", key);
                    return LookupResult.builder()
                            .cacheTTL(0L)
                            .build();
                }
                if (isResultEmpty(result)) {
                    LOG.trace("Empty lookup for key {} with TTL {}", key, ttlEmptyMillis());
                    return LookupResult.builder()
                            .cacheTTL(ttlEmptyMillis())
                            .build();
                }
                return result;
            } catch (Exception e) {
                LOG.warn("Loading value from data adapter failed for key {}, returning empty result", key, e);
                return LookupResult.withError(
                        String.format(Locale.ENGLISH, "Loading value from data adapter failed for key <%s>: %s", key.toString(), e.getMessage()));
            }
        };
        try (final Timer.Context ignored = lookupTimer()) {
            return cache.get(key, mapFunction);
        }
    }

    private boolean ignoreResult(LookupResult result, Boolean ignoreNull) {
        if (Boolean.TRUE.equals(ignoreNull)) {
            return isResultEmpty(result);
        }
        return false;
    }

    private boolean isResultEmpty(LookupResult result) {
        return (result == null ||
                (result.singleValue() == null && result.multiValue() == null && result.stringListValue() == null));
    }

    private long ttlEmptyMillis() {
        if (config.ttlEmpty() != null && config.ttlEmptyUnit() != null) {
            return config.ttlEmptyUnit().toMillis(config.ttlEmpty());
        }
        return Long.MAX_VALUE;
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
                    .ignoreNull(false)
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
        @JsonProperty(MAX_SIZE)
        public abstract int maxSize();

        @Min(0)
        @JsonProperty(EXPIRE_AFTER_ACCESS)
        public abstract long expireAfterAccess();

        @Nullable
        @JsonProperty(EXPIRE_AFTER_ACCESS_UNIT)
        public abstract TimeUnit expireAfterAccessUnit();

        @Min(0)
        @JsonProperty(EXPIRE_AFTER_WRITE)
        public abstract long expireAfterWrite();

        @Nullable
        @JsonProperty(EXPIRE_AFTER_WRITE_UNIT)
        public abstract TimeUnit expireAfterWriteUnit();

        @Nullable
        @JsonProperty(IGNORE_NULL)
        public abstract Boolean ignoreNull();

        @Min(0)
        @Nullable
        @JsonProperty(TTL_EMPTY)
        public abstract Long ttlEmpty();

        @Nullable
        @JsonProperty(TTL_EMPTY_UNIT)
        public abstract TimeUnit ttlEmptyUnit();

        public static Builder builder() {
            return new AutoValue_CaffeineLookupCache_Config.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty("type")
            public abstract Builder type(String type);

            @JsonProperty(MAX_SIZE)
            public abstract Builder maxSize(int maxSize);

            @JsonProperty(EXPIRE_AFTER_ACCESS)
            public abstract Builder expireAfterAccess(long expireAfterAccess);

            @JsonProperty(EXPIRE_AFTER_ACCESS_UNIT)
            public abstract Builder expireAfterAccessUnit(@Nullable TimeUnit expireAfterAccessUnit);

            @JsonProperty(EXPIRE_AFTER_WRITE)
            public abstract Builder expireAfterWrite(long expireAfterWrite);

            @JsonProperty(EXPIRE_AFTER_WRITE_UNIT)
            public abstract Builder expireAfterWriteUnit(@Nullable TimeUnit expireAfterWriteUnit);

            @JsonProperty(IGNORE_NULL)
            public abstract Builder ignoreNull(@Nullable Boolean ignoreNull);

            @JsonProperty(TTL_EMPTY)
            public abstract Builder ttlEmpty(@Nullable Long ttlEmpty);

            @JsonProperty(TTL_EMPTY_UNIT)
            public abstract Builder ttlEmptyUnit(@Nullable TimeUnit ttlEmptyUnit);

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
        public void recordLoadSuccess(long loadTime) {
            // not tracking this metric
        }

        @Override
        public void recordLoadFailure(long loadTime) {
            // not tracking this metric
        }

        @Override
        public void recordEviction(@NonNegative int i, RemovalCause removalCause) {
            // not tracking this metric
        }

        @Override
        public @NonNull CacheStats snapshot() {
            throw new UnsupportedOperationException("snapshots not implemented");
        }
    }
}
