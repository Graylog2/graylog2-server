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
package org.graylog2.bootstrap.preflight;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.bindings.providers.MongoConnectionProvider;
import org.graylog2.bootstrap.preflight.web.resources.PreflightResource;
import org.graylog2.bootstrap.preflight.web.resources.PreflightStatusResource;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.NodeServiceImpl;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.bindings.providers.ServiceManagerProvider;

public class PreflightWebModule extends Graylog2Module {

    public static final String FEATURE_FLAG_PREFLIGHT_CONFIG_ENABLED = "preflight_config";

    @Override
    protected void configure() {

        bind(ServiceManager.class).toProvider(ServiceManagerProvider.class).asEagerSingleton();
        bind(MongoConnection.class).toProvider(MongoConnectionProvider.class);
        bind(NodeService.class).to(NodeServiceImpl.class);

        bind(PreflightConfigService.class);

        addPreflightRestResource(PreflightResource.class);
        addPreflightRestResource(PreflightStatusResource.class);

        Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
        serviceBinder.addBinding().to(PreflightJerseyService.class);


        // needed for the ObjectMapperModule
        MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<MessageInput.Factory<? extends MessageInput>>() {
                });
    }

    protected void addPreflightRestResource(Class<?> restResourceClass) {
        preflightRestResourceBinder().addBinding().toInstance(restResourceClass);
    }

    private Multibinder<Class<?>> preflightRestResourceBinder() {
        return Multibinder.newSetBinder(
                binder(),
                new TypeLiteral<Class<?>>() {},
                PreflightRestResourcesBinding.class
        );
    }
}
