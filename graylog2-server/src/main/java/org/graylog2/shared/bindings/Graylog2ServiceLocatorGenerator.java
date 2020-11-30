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

import com.google.inject.Injector;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.jvnet.hk2.guice.bridge.api.HK2IntoGuiceBridge;

public class Graylog2ServiceLocatorGenerator extends ServiceLocatorGeneratorImpl {
    @Override
    public ServiceLocator create(String name, ServiceLocator parent) {
        final ServiceLocator serviceLocator = super.create(name, parent);

        final Injector injector = GuiceInjectorHolder.getInjector()
                .createChildInjector(new HK2IntoGuiceBridge(serviceLocator));

        GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
        final GuiceIntoHK2Bridge guiceBridge = serviceLocator.getService(GuiceIntoHK2Bridge.class);
        guiceBridge.bridgeGuiceInjector(injector);

        return serviceLocator;
    }
}
