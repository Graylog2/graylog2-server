/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
