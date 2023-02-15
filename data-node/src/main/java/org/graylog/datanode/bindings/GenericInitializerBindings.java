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
package org.graylog.datanode.bindings;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.graylog.datanode.configuration.DataNodeConfigProvider;
import org.graylog.datanode.configuration.DataNodeConfig;
import org.graylog.datanode.initializers.JerseyService;
import org.graylog.datanode.management.OpensearchProcessService;
import org.graylog.datanode.initializers.PeriodicalsService;
import org.graylog.datanode.configuration.ConfigurationProvider;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.datanode.management.OpensearchProcess;
import org.graylog.datanode.shutdown.GracefulShutdownService;

public class GenericInitializerBindings extends AbstractModule {
    @Override
    protected void configure() {
//        bind(ProcessingStatusRecorder.class).to(MongoDBProcessingStatusRecorderService.class).asEagerSingleton();


        bind(OpensearchConfiguration.class).toProvider(ConfigurationProvider.class);
        bind(DataNodeConfig.class).toProvider(DataNodeConfigProvider.class);

        Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);

        // this service both starts and provides the opensearch process
        serviceBinder.addBinding().to(OpensearchProcessService.class).asEagerSingleton();
        bind(OpensearchProcess.class).toProvider(OpensearchProcessService.class);

        serviceBinder.addBinding().to(PeriodicalsService.class);
        serviceBinder.addBinding().to(JerseyService.class);
        serviceBinder.addBinding().to(GracefulShutdownService.class).asEagerSingleton();

//        serviceBinder.addBinding().to(MongoDBProcessingStatusRecorderService.class).asEagerSingleton();
    }
}
