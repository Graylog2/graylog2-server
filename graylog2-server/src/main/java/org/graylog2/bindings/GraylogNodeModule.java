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
package org.graylog2.bindings;

import com.google.common.collect.Lists;
import com.google.inject.Scopes;
import org.graylog2.MinimalNodeConfiguration;
import org.graylog2.bootstrap.NodeSettings;
import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.system.FilePersistedNodeIdProvider;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.ObjectMapperModule;
import org.graylog2.shared.bindings.SchedulerBindings;

import java.util.List;

/**
 * <p>Guice module that contains all necessary bindings to start a basic node in a Graylog cluster.</p>
 *
 * <ul>
 *     <li>{@link ConfigurationModule} binds the basic config settings common to each node</li>
 *     <li>{@link MongoDbConnectionModule} binds the basic database connection and mongojack infrastructure</li>
 *     <li>{@link SchedulerBindings} binds the scheduled executors for daemon and non-daemon usage in the node</li>
 * </ul>
 */
public class GraylogNodeModule extends Graylog2Module {
    private final MinimalNodeConfiguration configuration;
    private final NodeSettings nodeSettings;

    public GraylogNodeModule(final MinimalNodeConfiguration configuration, NodeSettings nodeSettings) {
        this.configuration = configuration;
        this.nodeSettings = nodeSettings;
    }

    @Override
    protected void configure() {
        install(new MinimalNodeConfigurationModule(configuration));
//        install(new ServerStatusBindings(nodeSettings.capabilities()));
        if (nodeSettings.withMongoDb()) {
            install(new MongoDbConnectionModule());
            install(new ObjectMapperModule());
        }
//        install(new SchedulerBindings());
//        install(new AuditBindings());

//        if (nodeSettings.withEventBus()) {
//            bind(EventBus.class).toProvider(EventBusProvider.class).in(Scopes.SINGLETON);
//        }
        // ensure we always create a new LocalMetricRegistry, they are meant to be separate from each other
        bind(LocalMetricRegistry.class).in(Scopes.NO_SCOPE);
        bind(NodeId.class).toProvider(FilePersistedNodeIdProvider.class).asEagerSingleton();

        bind(EncryptedValueService.class).asEagerSingleton();
        bind(InputConfigurationBeanDeserializerModifier.class).toInstance(InputConfigurationBeanDeserializerModifier.withoutConfig());
    }

    public List<Object> getConfigurationBeans() {
        List<Object> configurationBeans = Lists.newArrayList(configuration);
        if (nodeSettings.withMongoDb()) {
            configurationBeans.add(new MongoDbConfiguration());
        }
        return configurationBeans;
    }
}
