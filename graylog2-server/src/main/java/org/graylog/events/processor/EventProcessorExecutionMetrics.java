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

package org.graylog.events.processor;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class EventProcessorExecutionMetrics {
    private final MetricRegistry metricRegistry;
    private final Map<String, Metric> metrics = new ConcurrentHashMap<>();

    private enum Fields {
        EXECUTION_COUNT("COUNTER"),
        EXECUTION_SUCCESSFUL("COUNTER"),
        EXECUTION_EXCEPTION("COUNTER"),
        EXECUTION_TIME("TIMER"),
        EVENTS_CREATED("METER");

        private final String type;
        Fields(String type) {
            this.type = type;
        }
        private String getType() {
            return type;
        }
    }
    @Inject
    public EventProcessorExecutionMetrics(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public void registerEventProcessor(EventProcessor eventProcessor, String definitionId) {
        for (Fields field: Fields.values() ) {
            final String name = getNameforField(eventProcessor, definitionId, field);
            switch (field.type) {
                case "COUNTER":
                    metrics.computeIfAbsent(name, k -> metricRegistry.counter(name));
                    break;
                case "TIMER":
                    metrics.computeIfAbsent(name, k -> metricRegistry.timer(name));
                    break;
                case "METER":
                    metrics.computeIfAbsent(name, k -> metricRegistry.meter(name));
                    break;
            }
        }
    }
    public void recordExecutionTime(EventProcessor eventProcessor, String definitionId, long time, TimeUnit unit) throws Exception {
        final String name = getNameforField(eventProcessor, definitionId, Fields.EXECUTION_TIME);
        this.metrics.computeIfAbsent(name, k -> metricRegistry.timer(name));
        ((Timer) metrics.get(name)).update(time, unit);
    }

    public void recordExecutions(EventProcessor eventProcessor, String definitionId) {
        final String name = getNameforField(eventProcessor, definitionId, Fields.EXECUTION_COUNT);
        this.metrics.computeIfAbsent(name, k -> metricRegistry.counter(name));
        ((Counter) metrics.get(name)).inc();
    }
    public void recordSuccess(EventProcessor eventProcessor, String definitionId) {
        final String name = getNameforField(eventProcessor, definitionId, Fields.EXECUTION_SUCCESSFUL);
        this.metrics.computeIfAbsent(name, k -> metricRegistry.counter(name));
        ((Counter) metrics.get(name)).inc();
    }
    public void recordException(EventProcessor eventProcessor, String definitionId) {
        final String name = getNameforField(eventProcessor, definitionId, Fields.EXECUTION_EXCEPTION);
        this.metrics.computeIfAbsent(name, k -> metricRegistry.counter(name));
        ((Counter) metrics.get(name)).inc();
    }
    public void recordCreatedEvents(EventProcessor eventProcessor, String definitionId, int count) {
        final String name = getNameforField(eventProcessor, definitionId, Fields.EVENTS_CREATED);
        this.metrics.computeIfAbsent(name, k -> metricRegistry.meter(name));
        ((Meter) metrics.get(name)).mark(count);
    }

    private String getNameforField(EventProcessor eventProcessor, String definitionId, Fields field) {
        return MetricRegistry.name(eventProcessor.getClass(), definitionId, field.toString().toLowerCase(Locale.ROOT));
    }
}
