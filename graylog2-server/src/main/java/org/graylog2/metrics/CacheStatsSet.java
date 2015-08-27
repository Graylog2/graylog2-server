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
package org.graylog2.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class CacheStatsSet implements MetricSet {
    // without type args, because the only call we care about (.stats()) is not generic
    private final Cache cache;

    public CacheStatsSet(Cache cache) {
        this.cache = checkNotNull(cache);
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final CacheStats cacheStats = cache.stats();
        return ImmutableMap.<String, Metric>builder()
                .put("requests", new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return cacheStats.requestCount();
                    }
                })
                .put("hits", new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return cacheStats.hitCount();
                    }
                })
                .put("misses", new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return cacheStats.missCount();
                    }
                })
                .put("evictions", new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return cacheStats.evictionCount();
                    }
                })
                .put("total-load-time-ns", new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return cacheStats.totalLoadTime();
                    }
                })
                .put("load-successes", new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return cacheStats.loadSuccessCount();
                    }
                })
                .put("load-exceptions", new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return cacheStats.loadExceptionCount();
                    }
                })
                .put("hit-rate", new Gauge<Double>() {
                    @Override
                    public Double getValue() {
                        return cacheStats.hitRate();
                    }
                })
                .put("miss-rate", new Gauge<Double>() {
                    @Override
                    public Double getValue() {
                        return cacheStats.missRate();
                    }
                })
                .build();
    }

}
