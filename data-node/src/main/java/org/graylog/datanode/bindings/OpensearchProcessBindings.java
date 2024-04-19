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
import org.graylog.datanode.configuration.OpensearchConfigurationProvider;
import org.graylog.datanode.metrics.ConfigureMetricsIndexSettings;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.datanode.opensearch.OpensearchProcessImpl;
import org.graylog.datanode.opensearch.OpensearchProcessService;
import org.graylog.datanode.opensearch.configuration.OpensearchConfiguration;
import org.graylog.datanode.opensearch.statemachine.OpensearchStateMachine;
import org.graylog.datanode.opensearch.statemachine.OpensearchStateMachineProvider;
import org.graylog.datanode.opensearch.statemachine.tracer.ClusterNodeStateTracer;
import org.graylog.datanode.opensearch.statemachine.tracer.OpensearchRemovalTracer;
import org.graylog.datanode.opensearch.statemachine.tracer.OpensearchWatchdog;
import org.graylog.datanode.opensearch.statemachine.tracer.StateMachineTracer;
import org.graylog.datanode.opensearch.statemachine.tracer.StateMachineTransitionLogger;

public class OpensearchProcessBindings extends AbstractModule {

    @Override
    protected void configure() {

        bind(OpensearchConfiguration.class).toProvider(OpensearchConfigurationProvider.class);
        Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);

        bind(OpensearchProcess.class).to(OpensearchProcessImpl.class).asEagerSingleton();
        bind(OpensearchStateMachine.class).toProvider(OpensearchStateMachineProvider.class).asEagerSingleton();
        // this service both starts and provides the opensearch process
        serviceBinder.addBinding().to(OpensearchProcessService.class).asEagerSingleton();

        // tracer
        Multibinder<StateMachineTracer> tracerBinder = Multibinder.newSetBinder(binder(), StateMachineTracer.class);
        tracerBinder.addBinding().to(ClusterNodeStateTracer.class).asEagerSingleton();
        tracerBinder.addBinding().to(OpensearchRemovalTracer.class).asEagerSingleton();
        tracerBinder.addBinding().to(OpensearchWatchdog.class).asEagerSingleton();
        tracerBinder.addBinding().to(StateMachineTransitionLogger.class).asEagerSingleton();
        tracerBinder.addBinding().to(ConfigureMetricsIndexSettings.class).asEagerSingleton();

    }

}
