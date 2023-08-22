package org.graylog2.lookup;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.assertj.core.api.Assertions;
import org.graylog2.lookup.caches.CaffeineLookupCache;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;
import org.graylog2.plugin.lookup.LookupCacheKey;
import org.graylog2.plugin.lookup.LookupResult;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
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


    @BeforeEach
    public void setUp() throws Exception {
    }

    @Test
    public void ignoreEmpty() throws Exception {
        when(registry.timer(anyString())).thenReturn(lookupTimer);
        when(registry.meter(anyString())).thenReturn(meter);

        LookupResult lr1 = LookupResult.empty();
        LookupResult lr2 = LookupResult.single("x");
        when(loader.call()).thenReturn(lr1).thenReturn(lr2);

        LookupCacheConfiguration config = CaffeineLookupCache.Config.builder()
                .type(CaffeineLookupCache.NAME)
                .maxSize(1000)
                .expireAfterAccess(60)
                .expireAfterAccessUnit(TimeUnit.SECONDS)
                .expireAfterWrite(60)
                .ignoreNull(true)
                .build();
        LookupCache cache = new CaffeineLookupCache("id", "name", config, 1, registry);

        LookupResult value1 = cache.get(LookupCacheKey.createFromJSON("x", "y"), loader);
        Assertions.assertThat(value1.singleValue()).isNull();

        LookupResult value2 = cache.get(LookupCacheKey.createFromJSON("x", "y"), loader);
        Assertions.assertThat(value2.singleValue()).isEqualTo("x");
    }

}
