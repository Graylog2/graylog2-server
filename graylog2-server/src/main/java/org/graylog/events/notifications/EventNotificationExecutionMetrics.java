/*
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

package org.graylog.events.notifications;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class EventNotificationExecutionMetrics {
    private final MetricRegistry metricRegistry;
    private final Map<String, Metric> metrics = new ConcurrentHashMap<>();
    private final Class CLASS = EventNotificationExecutionJob.class;
    private final String PREFIX = "executions";

    private enum Fields {
        TOTAL,
        SUCCESSFUL,
        FAILED_OTHER,
        FAILED_TEMPORARILY,
        FAILED_PERMANENTLY,
        IN_GRACE_PERIOD
    }

    @Inject
    public EventNotificationExecutionMetrics(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;

        for (Fields field: Fields.values() ) {
            final String name = getNameforField(field);
            this.metrics.put(name, metricRegistry.meter(name));
        }
    }

    public void markExecution() {
        getMeterforField(Fields.TOTAL).mark();
    }
    public void markSuccess() {
        getMeterforField(Fields.SUCCESSFUL).mark();
    }
    public void markInGrace() {
        getMeterforField(Fields.IN_GRACE_PERIOD).mark();
    }
    public void markFailedTemporarily() {
        getMeterforField(Fields.FAILED_TEMPORARILY).mark();
    }
    public void markFailedPermanently() {
        getMeterforField(Fields.FAILED_PERMANENTLY).mark();
    }
    public void markFailed() {
        getMeterforField(Fields.FAILED_OTHER).mark();
    }

    private String getNameforField(Fields field) {
        return MetricRegistry.name(CLASS, PREFIX, field.toString().toLowerCase(Locale.ROOT));
    }
    private Meter getMeterforField(Fields field) {
        return ((Meter) metrics.get(getNameforField(field)));
    }
}
