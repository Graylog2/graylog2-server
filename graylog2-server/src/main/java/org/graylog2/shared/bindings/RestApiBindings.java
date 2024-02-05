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
import org.graylog2.Configuration;
import org.graylog2.plugin.PluginModule;
import org.graylog2.rest.resources.RestResourcesModule;
import org.graylog2.shared.rest.resources.RestResourcesSharedModule;
import org.graylog2.shared.security.ShiroSecurityBinding;
import org.graylog2.web.IndexHtmlGenerator;
import org.graylog2.web.IndexHtmlGeneratorProvider;
import org.graylog2.web.resources.WebResourcesModule;

import jakarta.ws.rs.container.DynamicFeature;

public class RestApiBindings extends PluginModule {
    private final Configuration configuration;

    public RestApiBindings(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        bindDynamicFeatures();
        bindContainerResponseFilters();
        // just to create the binders so they are present in the injector
        // we don't actually have global REST API bindings for these
        jerseyExceptionMapperBinder();
        jerseyAdditionalComponentsBinder();

        // Ensure that we create the binder. We might not have any plugin that registers a JobResourceHandler.
        jobResourceHandlerBinder();

        bind(IndexHtmlGenerator.class).toProvider(IndexHtmlGeneratorProvider.class);

        // Install all resource modules
        install(new WebResourcesModule());
        install(new RestResourcesModule(configuration));
        install(new RestResourcesSharedModule());
    }

    private void bindDynamicFeatures() {
        Multibinder<Class<? extends DynamicFeature>> setBinder = jerseyDynamicFeatureBinder();
        setBinder.addBinding().toInstance(ShiroSecurityBinding.class);
    }

    private void bindContainerResponseFilters() {
        jerseyContainerResponseFilterBinder();
    }

}
