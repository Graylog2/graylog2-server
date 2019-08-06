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
import org.graylog2.shared.metrics.MetricUtils;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public abstract class EventProcessorExecutionMetrics {

    private enum Fields {
        EXECUTION_COUNT(new Counter()),
        EXECUTION_SUCCESSFUL(new Counter()),
        EXECUTION_EXCEPTION(new Counter()),
        EXECUTION_TIME(new Timer()),
        EVENTS_CREATED(new Meter());

        private final Metric type;
        Fields(Metric type) {
            this.type = type;
        }
    }

    static void registerEventProcessor(MetricRegistry metricRegistry, EventProcessor eventProcessor, String definitionId) {
        for (Fields field: Fields.values() ) {
            final String name = getNameforField(eventProcessor, definitionId, field);
            MetricUtils.safelyRegister(metricRegistry, name, field.type);
        }
    }
    static void recordExecutionTime(MetricRegistry registry, EventProcessor eventProcessor, String definitionId, long time, TimeUnit unit) throws Exception {
        final String name = getNameforField(eventProcessor, definitionId, Fields.EXECUTION_TIME);
        MetricUtils.getOrRegister(registry, name, new Timer()).update(time, unit);
    }

    static void recordExecutions(MetricRegistry registry, EventProcessor eventProcessor, String definitionId) {
        final String name = getNameforField(eventProcessor, definitionId, Fields.EXECUTION_COUNT);
        MetricUtils.getOrRegister(registry, name, new Counter()).inc();
    }
    static void recordSuccess(MetricRegistry registry, EventProcessor eventProcessor, String definitionId) {
        final String name = getNameforField(eventProcessor, definitionId, Fields.EXECUTION_SUCCESSFUL);
        MetricUtils.getOrRegister(registry, name, new Counter()).inc();
    }
    static void recordException(MetricRegistry registry, EventProcessor eventProcessor, String definitionId) {
        final String name = getNameforField(eventProcessor, definitionId, Fields.EXECUTION_EXCEPTION);
        MetricUtils.getOrRegister(registry, name, new Counter()).inc();
    }
    static void recordCreatedEvents(MetricRegistry registry, EventProcessor eventProcessor, String definitionId, int count) {
        final String name = getNameforField(eventProcessor, definitionId, Fields.EVENTS_CREATED);
        MetricUtils.getOrRegister(registry, name, new Meter()).mark(count);
    }

    private static String getNameforField(EventProcessor eventProcessor, String definitionId, Fields field) {
        return MetricRegistry.name(eventProcessor.getClass(), definitionId, field.toString().toLowerCase(Locale.ROOT));
    }
}
