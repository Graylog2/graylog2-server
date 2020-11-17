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
package org.graylog.scheduler;

import com.google.inject.multibindings.OptionalBinder;
import org.graylog.scheduler.audit.JobSchedulerAuditEventTypes;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.clock.JobSchedulerSystemClock;
import org.graylog.scheduler.eventbus.JobSchedulerEventBus;
import org.graylog.scheduler.eventbus.JobSchedulerEventBusProvider;
import org.graylog2.plugin.PluginModule;

/**
 * Job scheduler specific bindings.
 */
public class JobSchedulerModule extends PluginModule {
    @Override
    protected void configure() {
        bind(JobSchedulerService.class).asEagerSingleton();
        bind(JobSchedulerClock.class).toInstance(JobSchedulerSystemClock.INSTANCE);
        bind(JobSchedulerEventBus.class).toProvider(JobSchedulerEventBusProvider.class).asEagerSingleton();

        OptionalBinder.newOptionalBinder(binder(), JobSchedulerConfig.class)
                .setDefault().to(DefaultJobSchedulerConfig.class);

        // Add all rest resources in this package
        registerRestControllerPackage(getClass().getPackage().getName());

        addInitializer(JobSchedulerService.class);
        addAuditEventTypes(JobSchedulerAuditEventTypes.class);
    }
}
