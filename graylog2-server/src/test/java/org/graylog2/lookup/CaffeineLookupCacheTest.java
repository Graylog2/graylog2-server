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
import com.google.common.testing.FakeTicker;
import org.assertj.core.api.Assertions;
import org.graylog2.lookup.caches.CaffeineLookupCache;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;
import org.graylog2.plugin.lookup.LookupCacheKey;
import org.graylog2.plugin.lookup.LookupResult;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class CaffeineLookupCacheTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

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
        FakeTicker ticker = new FakeTicker();
        LookupCache cache = buildCache(ticker::read, 60, 600, 10);
        LookupResult lr1 = LookupResult.empty();
        LookupResult lr2 = LookupResult.single("x");
        when(loader.call()).thenReturn(lr1).thenReturn(lr2);

        LookupResult value1 = cache.get(LookupCacheKey.createFromJSON("x", "y"), loader);
        Assertions.assertThat(value1.singleValue()).isNull();

        ticker.advance(3, TimeUnit.SECONDS);
        LookupResult value2 = cache.get(LookupCacheKey.createFromJSON("x", "y"), loader);
        Assertions.assertThat(value2.singleValue()).isNull();

        ticker.advance(10, TimeUnit.SECONDS);
        LookupResult value3 = cache.get(LookupCacheKey.createFromJSON("x", "y"), loader);
        Assertions.assertThat(value3.singleValue()).isEqualTo("x");
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
                .ignoreNull(false)
                .ttlEmpty(ttlEmpty)
                .ttlEmptyUnit(TimeUnit.SECONDS)
                .build();
        return new CaffeineLookupCache("id", "name", config, registry, ticker);
    }
}
