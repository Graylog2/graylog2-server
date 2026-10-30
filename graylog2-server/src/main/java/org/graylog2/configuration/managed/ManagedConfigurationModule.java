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
package org.graylog2.configuration.managed;

import com.google.common.util.concurrent.Service;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.inject.Graylog2Module;

/**
 * Binds everything needed to read and change managed configuration parameters. Installed by the Graylog server and by
 * the data node, each passing its own node type, so both report their values with the same code.
 */
public class ManagedConfigurationModule extends Graylog2Module {
    private final NodeType nodeType;

    public ManagedConfigurationModule(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    @Override
    protected void configure() {
        bind(NodeType.class).toInstance(nodeType);
        bind(ConfigurationCatalog.class).asEagerSingleton();
        bind(ManagedConfigurationServices.class).asEagerSingleton();

        DefaultConfigurationParameters.all().forEach(this::addConfigurationParameter);

        final Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
        serviceBinder.addBinding().to(NodeConfigurationReporter.class).asEagerSingleton();
    }
}
