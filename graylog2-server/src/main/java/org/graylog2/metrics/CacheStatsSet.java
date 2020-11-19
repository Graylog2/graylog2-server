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
package org.graylog2.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;

public class CacheStatsSet implements MetricSet {
    private final Map<String, Metric> metrics;

    public CacheStatsSet(final String prefix, final Cache cache) {
        this.metrics = ImmutableMap.<String, Metric>builder()
                .put(name(prefix, "requests"), new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return cache.stats().requestCount();
                    }
                })
                .put(name(prefix, "hits"), new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return cache.stats().hitCount();
                    }
                })
                .put(name(prefix, "misses"), new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return cache.stats().missCount();
                    }
                })
                .put(name(prefix, "evictions"), new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return cache.stats().evictionCount();
                    }
                })
                .put(name(prefix, "total-load-time-ns"), new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return cache.stats().totalLoadTime();
                    }
                })
                .put(name(prefix, "load-successes"), new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return cache.stats().loadSuccessCount();
                    }
                })
                .put(name(prefix, "load-exceptions"), new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return cache.stats().loadExceptionCount();
                    }
                })
                .put(name(prefix, "hit-rate"), new Gauge<Double>() {
                    @Override
                    public Double getValue() {
                        return cache.stats().hitRate();
                    }
                })
                .put(name(prefix, "miss-rate"), new Gauge<Double>() {
                    @Override
                    public Double getValue() {
                        return cache.stats().missRate();
                    }
                })
                .build();
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return metrics;
    }
}
