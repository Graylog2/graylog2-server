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

import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.cluster.DataNodeServiceImpl;
import org.graylog.datanode.shared.system.activities.DataNodeActivityWriter;
import org.graylog2.bindings.providers.ClusterEventBusProvider;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.cluster.NodeService;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterIdFactory;
import org.graylog2.plugin.cluster.RandomUUIDClusterIdFactory;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.system.FilePersistedNodeIdProvider;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.system.activities.ActivityWriter;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.ext.ExceptionMapper;

public class ServerBindings extends Graylog2Module {
    private final Configuration configuration;
    private final boolean isMigrationCommand;

    public ServerBindings(Configuration configuration, boolean isMigrationCommand) {

        this.configuration = configuration;
        this.isMigrationCommand = isMigrationCommand;
    }

    @Override
    protected void configure() {
        bindInterfaces();
        bindSingletons();

        bindProviders();
        bindFactoryModules();
        bindDynamicFeatures();
        bindExceptionMappers();
        bindAdditionalJerseyComponents();
//        install(new AuthenticatingRealmModule(configuration));
//        install(new AuthorizationOnlyRealmModule());
    }

    private void bindProviders() {
        bind(ClusterEventBus.class).toProvider(ClusterEventBusProvider.class).asEagerSingleton();
        bind(NodeId.class).toProvider(FilePersistedNodeIdProvider.class).asEagerSingleton();
    }

    private void bindFactoryModules() {
        // System Jobs
    }

    private void bindSingletons() {
        bind(ClusterConfigService.class).to(ClusterConfigServiceImpl.class).asEagerSingleton();
    }

    private void bindInterfaces() {
        bind(ActivityWriter.class).to(DataNodeActivityWriter.class);
        OptionalBinder.newOptionalBinder(binder(), ClusterIdFactory.class).setDefault().to(RandomUUIDClusterIdFactory.class);
        bind(NodeService.class).to(DataNodeServiceImpl.class);
    }

    private void bindDynamicFeatures() {
        final Multibinder<Class<? extends DynamicFeature>> dynamicFeatures = jerseyDynamicFeatureBinder();
    }

    private void bindExceptionMappers() {
        final Multibinder<Class<? extends ExceptionMapper>> exceptionMappers = jerseyExceptionMapperBinder();
    }

    private void bindAdditionalJerseyComponents() {
//        jerseyAdditionalComponentsBinder().addBinding().toInstance(GenericErrorCsvWriter.class);
    }
}
