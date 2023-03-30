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
import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.shared.bindings.providers.ServiceManagerProvider;

public class PreflightWebModule extends Graylog2Module {

    public static final String FEATURE_FLAG_PREFLIGHT_CONFIG_ENABLED = "preflight_config";

    @Override
    protected void configure() {

        bind(ServiceManager.class).toProvider(ServiceManagerProvider.class).asEagerSingleton();

        bind(PreflightConfigService.class);

        addSystemRestResource(PreflightStatusController.class);

        Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
        serviceBinder.addBinding().to(PreflightJerseyService.class);
    }
}
