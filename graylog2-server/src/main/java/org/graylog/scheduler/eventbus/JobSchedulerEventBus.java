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
package org.graylog.scheduler.eventbus;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.eventbus.EventBus;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Job scheduler specific event bus instance. This is a <em>synchronous</em> event bus.
 * Subscribers must ensure that the callback method is fast or put expensive work into an executor or queue.
 */
public class JobSchedulerEventBus extends EventBus {
    private final Counter registrationsCount;
    private final Timer postTimer;

    @SuppressWarnings("WeakerAccess")
    public JobSchedulerEventBus(String name, MetricRegistry metricRegistry) {
        super(name);
        this.registrationsCount = metricRegistry.counter(name(getClass(), name, "registrations"));
        this.postTimer = metricRegistry.timer(name(getClass(), name, "posts"));
    }

    @Override
    public void register(Object object) {
        registrationsCount.inc();
        super.register(object);
    }

    @Override
    public void unregister(Object object) {
        registrationsCount.dec();
        super.unregister(object);
    }

    @Override
    public void post(Object event) {
        try (final Timer.Context ignored = postTimer.time()) {
            super.post(event);
        }
    }
}
