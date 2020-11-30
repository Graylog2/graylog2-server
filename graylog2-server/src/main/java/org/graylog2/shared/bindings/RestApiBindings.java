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

import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.shared.security.ShiroSecurityBinding;
import org.graylog2.web.DevelopmentIndexHtmlGenerator;
import org.graylog2.web.IndexHtmlGenerator;
import org.graylog2.web.ProductionIndexHtmlGenerator;

import javax.ws.rs.container.DynamicFeature;

public class RestApiBindings extends Graylog2Module {
    @Override
    protected void configure() {
        bindDynamicFeatures();
        bindContainerResponseFilters();
        // just to create the binders so they are present in the injector
        // we don't actually have global REST API bindings for these
        jerseyExceptionMapperBinder();
        jerseyAdditionalComponentsBinder();

        // In development mode we use an external process to provide the web interface.
        // To avoid errors because of missing production web assets, we use a different implementation for
        // generating the "index.html" page.
        if (System.getenv("DEVELOPMENT") == null) {
            bind(IndexHtmlGenerator.class).to(ProductionIndexHtmlGenerator.class).asEagerSingleton();
        } else {
            bind(IndexHtmlGenerator.class).to(DevelopmentIndexHtmlGenerator.class).asEagerSingleton();
        }
    }

    private void bindDynamicFeatures() {
        Multibinder<Class<? extends DynamicFeature>> setBinder = jerseyDynamicFeatureBinder();
        setBinder.addBinding().toInstance(ShiroSecurityBinding.class);
    }

    private void bindContainerResponseFilters() {
        jerseyContainerResponseFilterBinder();
    }

}
