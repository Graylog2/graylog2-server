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
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.graylog.datanode.configuration.DatanodeTrustManagerProvider;
import org.graylog.datanode.configuration.OpensearchConfigurationService;
import org.graylog.datanode.configuration.variants.DatanodeKeystoreOpensearchCertificatesProvider;
import org.graylog.datanode.configuration.variants.LocalConfigurationCertificatesProvider;
import org.graylog.datanode.configuration.variants.NoOpensearchCertificatesProvider;
import org.graylog.datanode.configuration.variants.OpensearchCertificatesProvider;
import org.graylog.datanode.metrics.ConfigureMetricsIndexSettings;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.datanode.opensearch.OpensearchProcessImpl;
import org.graylog.datanode.opensearch.OpensearchProcessService;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.opensearch.configuration.OpensearchUsableSpace;
import org.graylog.datanode.opensearch.configuration.OpensearchUsableSpaceProvider;
import org.graylog.datanode.opensearch.configuration.beans.impl.OpensearchClusterConfigurationBean;
import org.graylog.datanode.opensearch.configuration.beans.impl.OpensearchCommonConfigurationBean;
import org.graylog.datanode.opensearch.configuration.beans.impl.OpensearchDefaultConfigFilesBean;
import org.graylog.datanode.opensearch.configuration.beans.impl.OpensearchSecurityConfigurationBean;
import org.graylog.datanode.opensearch.configuration.beans.impl.SearchableSnapshotsConfigurationBean;
import org.graylog.datanode.opensearch.statemachine.OpensearchStateMachine;
import org.graylog.datanode.opensearch.statemachine.OpensearchStateMachineProvider;
import org.graylog.datanode.opensearch.statemachine.tracer.ClusterNodeStateTracer;
import org.graylog.datanode.opensearch.statemachine.tracer.OpensearchWatchdog;
import org.graylog.datanode.opensearch.statemachine.tracer.StateMachineTracer;
import org.graylog.datanode.opensearch.statemachine.tracer.StateMachineTransitionLogger;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationBean;

public class OpensearchProcessBindings extends AbstractModule {

    @Override
    protected void configure() {


        Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);

        bind(OpensearchProcess.class).to(OpensearchProcessImpl.class).asEagerSingleton();
        bind(OpensearchStateMachine.class).toProvider(OpensearchStateMachineProvider.class).asEagerSingleton();

        bind(OpensearchUsableSpace.class).toProvider(OpensearchUsableSpaceProvider.class).asEagerSingleton();

        //opensearch certificate providers
        Multibinder<OpensearchCertificatesProvider> opensearchCertificatesProviders = Multibinder.newSetBinder(binder(), OpensearchCertificatesProvider.class);
        opensearchCertificatesProviders.addBinding().to(LocalConfigurationCertificatesProvider.class).asEagerSingleton();
        opensearchCertificatesProviders.addBinding().to(DatanodeKeystoreOpensearchCertificatesProvider.class).asEagerSingleton();
        opensearchCertificatesProviders.addBinding().to(NoOpensearchCertificatesProvider.class).asEagerSingleton();


        //opensearch configuration beans. The order of the beans is important here!

        Multibinder<DatanodeConfigurationBean<OpensearchConfigurationParams>> opensearchConfigurationBeanMultibinder = Multibinder.newSetBinder(binder(), new TypeLiteral<DatanodeConfigurationBean<OpensearchConfigurationParams>>() {});
        opensearchConfigurationBeanMultibinder.addBinding().to(OpensearchDefaultConfigFilesBean.class).asEagerSingleton();
        opensearchConfigurationBeanMultibinder.addBinding().to(OpensearchCommonConfigurationBean.class).asEagerSingleton();
        opensearchConfigurationBeanMultibinder.addBinding().to(OpensearchClusterConfigurationBean.class).asEagerSingleton();
        opensearchConfigurationBeanMultibinder.addBinding().to(SearchableSnapshotsConfigurationBean.class).asEagerSingleton();
        opensearchConfigurationBeanMultibinder.addBinding().to(OpensearchSecurityConfigurationBean.class).asEagerSingleton();

        // this service both starts and provides the opensearch process
        serviceBinder.addBinding().to(OpensearchConfigurationService.class).asEagerSingleton();
        serviceBinder.addBinding().to(OpensearchProcessService.class).asEagerSingleton();

        bind(DatanodeTrustManagerProvider.class);

        // tracer
        Multibinder<StateMachineTracer> tracerBinder = Multibinder.newSetBinder(binder(), StateMachineTracer.class);
        tracerBinder.addBinding().to(ClusterNodeStateTracer.class).asEagerSingleton();
        tracerBinder.addBinding().to(OpensearchWatchdog.class).asEagerSingleton();
        tracerBinder.addBinding().to(StateMachineTransitionLogger.class).asEagerSingleton();
        tracerBinder.addBinding().to(ConfigureMetricsIndexSettings.class).asEagerSingleton();

    }

}
