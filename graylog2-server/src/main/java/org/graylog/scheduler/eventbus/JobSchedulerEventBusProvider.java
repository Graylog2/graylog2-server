package org.graylog.scheduler.eventbus;

import com.codahale.metrics.MetricRegistry;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Creates a {@link JobSchedulerEventBus} instance.
 */
public class JobSchedulerEventBusProvider implements Provider<JobSchedulerEventBus> {
    private final MetricRegistry metricRegistry;

    @Inject
    public JobSchedulerEventBusProvider(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public JobSchedulerEventBus get() {
        return new JobSchedulerEventBus("system", metricRegistry);
    }
}
