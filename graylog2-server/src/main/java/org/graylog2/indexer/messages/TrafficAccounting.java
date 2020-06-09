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
package org.graylog2.indexer.messages;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.GlobalMetricNames;

import javax.inject.Inject;

public class TrafficAccounting {
    private final Counter outputByteCounter;
    private final Counter systemTrafficCounter;

    @Inject
    public TrafficAccounting(MetricRegistry metricRegistry) {
        outputByteCounter = metricRegistry.counter(GlobalMetricNames.OUTPUT_TRAFFIC);
        systemTrafficCounter = metricRegistry.counter(GlobalMetricNames.SYSTEM_OUTPUT_TRAFFIC);
    }

    public void addOutputTraffic(long size) {
        this.outputByteCounter.inc(size);
    }

    public void addSystemTraffic(long size) {
        this.systemTrafficCounter.inc(size);
    }
}
