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
import com.google.common.collect.Maps;

import java.util.Map;

public class CacheStatsSet implements MetricSet {
    // without type args, because the only call we care about (.stats()) is not generic
    private final Cache cache;

    public CacheStatsSet(Cache cache) {
        this.cache = cache;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = Maps.newHashMap();
        final CacheStats cacheStats = cache.stats();

        gauges.put("hits", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return cacheStats.hitCount();
            }
        });
        gauges.put("misses", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return cacheStats.missCount();
            }
        });
        gauges.put("evictions", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return cacheStats.evictionCount();
            }
        });
        gauges.put("total-load-time-ns", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return cacheStats.totalLoadTime();
            }
        });
        gauges.put("load-successes", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return cacheStats.loadSuccessCount();
            }
        });
        gauges.put("load-exceptions", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return cacheStats.loadExceptionCount();
            }
        });
        gauges.put("hits", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return cacheStats.hitCount();
            }
        });

        return gauges;
    }

}
