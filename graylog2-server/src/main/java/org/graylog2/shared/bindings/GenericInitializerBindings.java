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
package org.graylog2.shared.bindings;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.shared.initializers.InputSetupService;
import org.graylog2.shared.initializers.JerseyService;
import org.graylog2.shared.initializers.PeriodicalsService;
import org.graylog2.system.processing.MongoDBProcessingStatusRecorderService;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.graylog2.system.shutdown.GracefulShutdownService;

public class GenericInitializerBindings extends AbstractModule {
    @Override
    protected void configure() {
        bind(ProcessingStatusRecorder.class).to(MongoDBProcessingStatusRecorderService.class).asEagerSingleton();

        Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
        serviceBinder.addBinding().to(InputSetupService.class);
        serviceBinder.addBinding().to(PeriodicalsService.class);
        serviceBinder.addBinding().to(JerseyService.class);
        serviceBinder.addBinding().to(GracefulShutdownService.class).asEagerSingleton();
        serviceBinder.addBinding().to(MongoDBProcessingStatusRecorderService.class).asEagerSingleton();
    }
}
