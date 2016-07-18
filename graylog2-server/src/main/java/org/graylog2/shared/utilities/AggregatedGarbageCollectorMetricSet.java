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
package org.graylog2.shared.utilities;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Returns aggregated GC status.
 *
 * {@link java.lang.management.ManagementFactory#getGarbageCollectorMXBeans()} typically returns several garbage
 * collectors that must be queried separately. This class aggregates sums of the available statistics.
 *
 * Notes:
 * <ul>
 *     <li>GarbageCollectorMXBean does not report for how long the world has been stopped during GC</li>
 *     <li>More detailed information is required for debugging memory management issues</li>
 *     <li>verbosegc is probably still the best bet for getting detailed, and reliable information</li>
 *     <li>It is not guaranteed that all nodes in Graylog cluster use the same garbage collector</li>
 * </ul>
 *
 */
public class AggregatedGarbageCollectorMetricSet implements MetricSet {
    private final List<GarbageCollectorMXBean> garbageCollectors;

    public AggregatedGarbageCollectorMetricSet() {
        this(ManagementFactory.getGarbageCollectorMXBeans());
    }

    public AggregatedGarbageCollectorMetricSet(Collection<GarbageCollectorMXBean> garbageCollectors) {
        this.garbageCollectors = new ArrayList(garbageCollectors);
    }

    public Map<String, Metric> getMetrics() {
        HashMap gauges = new HashMap();

        gauges.put(MetricRegistry.name("count"), new Gauge() {
            public Long getValue() {
                return garbageCollectors.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
            }
        });

        gauges.put(MetricRegistry.name("time"), new Gauge() {
            public Long getValue() {
                return garbageCollectors.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
            }
        });

        return Collections.unmodifiableMap(gauges);

    }
}
