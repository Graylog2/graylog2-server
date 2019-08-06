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

package org.graylog.events.processor;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.graylog2.shared.metrics.MetricUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Singleton
public class EventProcessorExecutionMetrics {
    private final MetricRegistry metricRegistry;

    @Inject
    public EventProcessorExecutionMetrics(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    private enum Fields {
        EXECUTION_COUNT(Counter::new),
        EXECUTION_SUCCESSFUL(Counter::new),
        EXECUTION_EXCEPTION(Counter::new),
        EXECUTION_TIME(Timer::new),
        EVENTS_CREATED(Meter::new);

        private final Supplier<Metric> type;

        Fields(Supplier<Metric> type) {
            this.type = type;
        }
    }

    void registerEventProcessor(EventProcessor eventProcessor, String definitionId) {
        for (Fields field : Fields.values()) {
            final String name = getNameForField(eventProcessor, definitionId, field);
            MetricUtils.safelyRegister(metricRegistry, name, field.type.get());
        }
    }

    void recordExecutionTime(EventProcessor eventProcessor, String definitionId, Duration duration) {
        final String name = getNameForField(eventProcessor, definitionId, Fields.EXECUTION_TIME);
        MetricUtils.getOrRegister(metricRegistry, name, new Timer()).update(duration.getNano(), TimeUnit.NANOSECONDS);
    }

    void recordExecutions(EventProcessor eventProcessor, String definitionId) {
        final String name = getNameForField(eventProcessor, definitionId, Fields.EXECUTION_COUNT);
        MetricUtils.getOrRegister(metricRegistry, name, new Counter()).inc();
    }

    void recordSuccess(EventProcessor eventProcessor, String definitionId) {
        final String name = getNameForField(eventProcessor, definitionId, Fields.EXECUTION_SUCCESSFUL);
        MetricUtils.getOrRegister(metricRegistry, name, new Counter()).inc();
    }

    void recordException(EventProcessor eventProcessor, String definitionId) {
        final String name = getNameForField(eventProcessor, definitionId, Fields.EXECUTION_EXCEPTION);
        MetricUtils.getOrRegister(metricRegistry, name, new Counter()).inc();
    }

    void recordCreatedEvents(EventProcessor eventProcessor, String definitionId, int count) {
        final String name = getNameForField(eventProcessor, definitionId, Fields.EVENTS_CREATED);
        MetricUtils.getOrRegister(metricRegistry, name, new Meter()).mark(count);
    }

    private static String getNameForField(EventProcessor eventProcessor, String definitionId, Fields field) {
        return MetricRegistry.name(eventProcessor.getClass(), definitionId, field.toString().toLowerCase(Locale.ROOT));
    }
}
