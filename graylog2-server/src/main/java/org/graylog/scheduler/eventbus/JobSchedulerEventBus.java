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
    private static final String METRICS_PREFIX = "job-scheduler-eventbus";

    private final Counter registrationsCount;
    private final Timer postTimer;

    @SuppressWarnings("WeakerAccess")
    public JobSchedulerEventBus(String name, MetricRegistry metricRegistry) {
        super(name);
        this.registrationsCount = metricRegistry.counter(name(METRICS_PREFIX, name, "registrations"));
        this.postTimer = metricRegistry.timer(name(METRICS_PREFIX, name, "posts"));
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
