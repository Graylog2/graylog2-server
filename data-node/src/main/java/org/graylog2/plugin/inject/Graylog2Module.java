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
package org.graylog2.plugin.inject;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import javax.annotation.Nonnull;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.ext.ExceptionMapper;

public abstract class Graylog2Module extends AbstractModule {
    public static final String SYSTEM_REST_RESOURCES = "systemRestResources";

    private static class DynamicFeatureType extends TypeLiteral<Class<? extends DynamicFeature>> {}

    private static class ContainerResponseFilterType extends TypeLiteral<Class<? extends ContainerResponseFilter>> {}

    private static class ExceptionMapperType extends TypeLiteral<Class<? extends ExceptionMapper>> {}

    @Nonnull
    protected Multibinder<Class<? extends DynamicFeature>> jerseyDynamicFeatureBinder() {
        return Multibinder.newSetBinder(binder(), new DynamicFeatureType());
    }

    @Nonnull
    protected Multibinder<Class<? extends ContainerResponseFilter>> jerseyContainerResponseFilterBinder() {
        return Multibinder.newSetBinder(binder(), new ContainerResponseFilterType());
    }

    @Nonnull
    protected Multibinder<Class<? extends ExceptionMapper>> jerseyExceptionMapperBinder() {
        return Multibinder.newSetBinder(binder(), new ExceptionMapperType());
    }

    @Nonnull
    protected Multibinder<Class> jerseyAdditionalComponentsBinder() {
        return Multibinder.newSetBinder(binder(), Class.class, Names.named("additionalJerseyComponents"));
    }

    /**
     * Adds given API resource as a system resource. This should not be used from plugins!
     * Plugins should use {@link org.graylog2.plugin.PluginModule#addRestResource(Class)} instead to ensure the
     * addition of the path prefix.
     *
     * @param restResourceClass the resource to add
     */
    protected void addSystemRestResource(Class<?> restResourceClass) {
        systemRestResourceBinder().addBinding().toInstance(restResourceClass);
    }

    private Multibinder<Class<?>> systemRestResourceBinder() {
        return Multibinder.newSetBinder(
                binder(),
                new TypeLiteral<Class<?>>() {},
                Names.named(SYSTEM_REST_RESOURCES)
        );
    }
}
