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
package org.graylog2.lookup;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.benmanes.caffeine.cache.Ticker;
import org.assertj.core.api.Assertions;
import org.graylog2.lookup.caches.CaffeineLookupCache;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;
import org.graylog2.plugin.lookup.LookupCacheKey;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.utilities.FakeTicker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.graylog2.plugin.lookup.LookupResult.DEFAULT_ERROR_LOOKUP_RESULT;
import static org.graylog2.plugin.lookup.LookupResult.EMPTY_LOOKUP_RESULT;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class CaffeineLookupCacheTest {

    @Mock
    MetricRegistry registry;
    @Mock
    Timer lookupTimer;
    @Mock
    Meter meter;
    @Mock
    Callable<LookupResult> loader;

    @Test
    public void ignoreEmpty() throws Exception {
        LookupCache cache = buildCache(true);
        LookupResult lr1 = LookupResult.empty();
        LookupResult lr2 = LookupResult.single("x");
        when(loader.call()).thenReturn(lr1).thenReturn(lr2);

        LookupResult value1 = cache.get(LookupCacheKey.createFromJSON("x", "y"), loader);
        Assertions.assertThat(value1.singleValue()).isNull();

        LookupResult value2 = cache.get(LookupCacheKey.createFromJSON("x", "y"), loader);
        Assertions.assertThat(value2.singleValue()).isEqualTo("x");
    }

    @Test
    public void cacheNonEmpty() throws Exception {
        LookupCache cache = buildCache(true);
        LookupResult lr1 = LookupResult.single("x1");
        LookupResult lr2 = LookupResult.single("x2");
        when(loader.call()).thenReturn(lr1).thenReturn(lr2);

        LookupResult value1 = cache.get(LookupCacheKey.createFromJSON("x", "y"), loader);
        Assertions.assertThat(value1.singleValue()).isEqualTo("x1");

        LookupResult value2 = cache.get(LookupCacheKey.createFromJSON("x", "y"), loader);
        Assertions.assertThat(value2.singleValue()).isEqualTo("x1");
    }

    @Test
    public void cacheEmpty() throws Exception {
        LookupCache cache = buildCache(false);
        LookupResult lr1 = LookupResult.empty();
        LookupResult lr2 = LookupResult.single("x");
        when(loader.call()).thenReturn(lr1).thenReturn(lr2);

        LookupResult value1 = cache.get(LookupCacheKey.createFromJSON("x", "y"), loader);
        Assertions.assertThat(value1.singleValue()).isNull();

        LookupResult value2 = cache.get(LookupCacheKey.createFromJSON("x", "y"), loader);
        Assertions.assertThat(value1.singleValue()).isNull();
    }

    @SuppressWarnings("UnstableApiUsage")
    @Test
    public void ttlEmpty() throws Exception {
        FakeTicker ticker = new FakeTicker(Duration.ZERO);
        LookupCache cache = buildCache(ticker, 60, 600, 10);
        LookupResult lr1 = LookupResult.empty();
        LookupResult lr2 = LookupResult.single("x");
        when(loader.call()).thenReturn(lr1).thenReturn(lr2);

        LookupResult value1 = cache.get(LookupCacheKey.createFromJSON("x", "y"), loader);
        Assertions.assertThat(value1.singleValue()).isNull();

        ticker.advance(Duration.ofSeconds(3));
        LookupResult value2 = cache.get(LookupCacheKey.createFromJSON("x", "y"), loader);
        Assertions.assertThat(value2.singleValue()).isNull();

        ticker.advance(Duration.ofSeconds(10));
        LookupResult value3 = cache.get(LookupCacheKey.createFromJSON("x", "y"), loader);
        Assertions.assertThat(value3.singleValue()).isEqualTo("x");
    }

    @Test
    public void readsDoNotExtendPastResultTtl() throws Exception {
        FakeTicker ticker = new FakeTicker(Duration.ZERO);
        LookupCache cache = buildCache(ticker, 60, 0, 10);
        LookupResult lr1 = LookupResult.builder().single("x1").cacheTTL(TimeUnit.SECONDS.toMillis(10)).build();
        LookupResult lr2 = LookupResult.single("x2");
        when(loader.call()).thenReturn(lr1).thenReturn(lr2);
        final LookupCacheKey key = LookupCacheKey.createFromJSON("x", "y");

        Assertions.assertThat(cache.get(key, loader).singleValue()).isEqualTo("x1");

        ticker.advance(Duration.ofSeconds(5));
        Assertions.assertThat(cache.get(key, loader).singleValue()).isEqualTo("x1");

        ticker.advance(Duration.ofSeconds(4));
        Assertions.assertThat(cache.get(key, loader).singleValue()).isEqualTo("x1");

        // Each read above would have reset expiry to the 60s access window; the 10s TTL must still win.
        ticker.advance(Duration.ofSeconds(2));
        Assertions.assertThat(cache.get(key, loader).singleValue()).isEqualTo("x2");
    }

    @Test
    public void readsDoNotExtendPastResultTtlAcrossTickerRollover() throws Exception {
        FakeTicker ticker = new FakeTicker(Duration.ZERO);
        ticker.advance(Long.MAX_VALUE - Duration.ofSeconds(2).toNanos());
        LookupCache cache = buildCache(ticker, 60, 0, 10);
        LookupResult lr1 = LookupResult.builder().single("x1").cacheTTL(TimeUnit.SECONDS.toMillis(5)).build();
        LookupResult lr2 = LookupResult.single("x2");
        when(loader.call()).thenReturn(lr1).thenReturn(lr2);
        final LookupCacheKey key = LookupCacheKey.createFromJSON("x", "y");

        Assertions.assertThat(cache.get(key, loader).singleValue()).isEqualTo("x1");

        // Reads on both sides of the ticker wrapping past Long.MAX_VALUE stay within the 5s TTL.
        for (int i = 0; i < 4; i++) {
            ticker.advance(Duration.ofSeconds(1));
            Assertions.assertThat(cache.get(key, loader).singleValue()).isEqualTo("x1");
        }

        ticker.advance(Duration.ofSeconds(2));
        Assertions.assertThat(cache.get(key, loader).singleValue()).isEqualTo("x2");
    }

    @Test
    public void readsDoNotExtendPastExpireAfterWrite() throws Exception {
        FakeTicker ticker = new FakeTicker(Duration.ZERO);
        LookupCache cache = buildCache(ticker, 60, 30, 10);
        LookupResult lr1 = LookupResult.single("x1");
        LookupResult lr2 = LookupResult.single("x2");
        when(loader.call()).thenReturn(lr1).thenReturn(lr2);
        final LookupCacheKey key = LookupCacheKey.createFromJSON("x", "y");

        Assertions.assertThat(cache.get(key, loader).singleValue()).isEqualTo("x1");

        ticker.advance(Duration.ofSeconds(20));
        Assertions.assertThat(cache.get(key, loader).singleValue()).isEqualTo("x1");

        ticker.advance(Duration.ofSeconds(20));
        Assertions.assertThat(cache.get(key, loader).singleValue()).isEqualTo("x2");
    }

    @Test
    public void resultTtlDoesNotExtendPastExpireAfterWrite() throws Exception {
        FakeTicker ticker = new FakeTicker(Duration.ZERO);
        LookupCache cache = buildCache(ticker, 0, 30, 10);
        LookupResult lr1 = LookupResult.builder().single("x1").cacheTTL(TimeUnit.HOURS.toMillis(1)).build();
        LookupResult lr2 = LookupResult.single("x2");
        when(loader.call()).thenReturn(lr1).thenReturn(lr2);
        final LookupCacheKey key = LookupCacheKey.createFromJSON("x", "y");

        Assertions.assertThat(cache.get(key, loader).singleValue()).isEqualTo("x1");

        ticker.advance(Duration.ofSeconds(29));
        Assertions.assertThat(cache.get(key, loader).singleValue()).isEqualTo("x1");

        ticker.advance(Duration.ofSeconds(2));
        Assertions.assertThat(cache.get(key, loader).singleValue()).isEqualTo("x2");
    }

    @Test
    public void expireAfterAccessStillEvictsIdleTtlResults() throws Exception {
        FakeTicker ticker = new FakeTicker(Duration.ZERO);
        LookupCache cache = buildCache(ticker, 60, 0, 10);
        LookupResult lr1 = LookupResult.builder().single("x1").cacheTTL(TimeUnit.HOURS.toMillis(1)).build();
        LookupResult lr2 = LookupResult.single("x2");
        when(loader.call()).thenReturn(lr1).thenReturn(lr2);
        final LookupCacheKey key = LookupCacheKey.createFromJSON("x", "y");

        Assertions.assertThat(cache.get(key, loader).singleValue()).isEqualTo("x1");

        ticker.advance(Duration.ofSeconds(1));
        Assertions.assertThat(cache.get(key, loader).singleValue()).isEqualTo("x1");

        ticker.advance(Duration.ofSeconds(61));
        Assertions.assertThat(cache.get(key, loader).singleValue()).isEqualTo("x2");
    }

    @Test
    public void ttlError() throws Exception {
        LookupCache cache = buildCache(false);
        LookupResult lr1 = LookupResult.empty();
        LookupResult lr2 = LookupResult.withError();
        LookupResult lr3 = LookupResult.withError(999);
        when(loader.call()).thenReturn(lr1).thenReturn(lr2).thenReturn(lr3);

        LookupResult value1 = cache.get(LookupCacheKey.createFromJSON("x1", "y"), loader);
        Assertions.assertThat(value1.singleValue()).isNull();
        Assertions.assertThat(value1.cacheTTL()).isEqualTo(EMPTY_LOOKUP_RESULT.cacheTTL());

        LookupResult value2 = cache.get(LookupCacheKey.createFromJSON("x2", "y"), loader);
        Assertions.assertThat(value2.singleValue()).isNull();
        Assertions.assertThat(value2.cacheTTL()).isEqualTo(DEFAULT_ERROR_LOOKUP_RESULT.cacheTTL());

        LookupResult value3 = cache.get(LookupCacheKey.createFromJSON("x3", "y"), loader);
        Assertions.assertThat(value3.singleValue()).isNull();
        Assertions.assertThat(value3.cacheTTL()).isEqualTo(999);
    }

    private LookupCache buildCache(boolean ignoreNull) {
        when(registry.timer(anyString())).thenReturn(lookupTimer);
        when(registry.meter(anyString())).thenReturn(meter);

        LookupCacheConfiguration config = CaffeineLookupCache.Config.builder()
                .type(CaffeineLookupCache.NAME)
                .maxSize(1000)
                .expireAfterAccess(60)
                .expireAfterAccessUnit(TimeUnit.SECONDS)
                .expireAfterWrite(60)
                .ignoreNull(ignoreNull)
                .build();
        return new CaffeineLookupCache("id", "name", config, registry);
    }

    private LookupCache buildCache(Ticker ticker, long expireAfterAccess, long expireAfterWrite, long ttlEmpty) {
        when(registry.timer(anyString())).thenReturn(lookupTimer);
        when(registry.meter(anyString())).thenReturn(meter);

        CaffeineLookupCache.Config config = CaffeineLookupCache.Config.builder()
                .type(CaffeineLookupCache.NAME)
                .maxSize(1000)
                .expireAfterAccess(expireAfterAccess)
                .expireAfterAccessUnit(TimeUnit.SECONDS)
                .expireAfterWrite(expireAfterWrite)
                .expireAfterWriteUnit(TimeUnit.SECONDS)
                .ignoreNull(false)
                .ttlEmpty(ttlEmpty)
                .ttlEmptyUnit(TimeUnit.SECONDS)
                .build();
        return new CaffeineLookupCache("id", "name", config, registry, ticker);
    }
}
