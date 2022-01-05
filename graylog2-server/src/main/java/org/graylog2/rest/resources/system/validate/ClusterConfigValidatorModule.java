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

package org.graylog2.rest.resources.system.validate;

import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import org.graylog.plugins.map.config.GeoIpResolverConfig;
import org.graylog2.plugin.PluginModule;

public class ClusterConfigValidatorModule extends PluginModule {
    @Override
    public void configure() {

        addClusterConfigValidator(GeoIpResolverConfig.class, GeoIpResolverConfigValidator.class);
    }

    private void addClusterConfigValidator(Class<?> configClass, Class<? extends ClusterConfigValidator> configValidatorClass) {

        mapBinder().addBinding(configClass).to(configValidatorClass);

    }

    private MapBinder<Class<?>, ClusterConfigValidator> mapBinder() {
        TypeLiteral<Class<?>> keyType = new TypeLiteral<Class<?>>() {};
        TypeLiteral<ClusterConfigValidator> valueType = new TypeLiteral<ClusterConfigValidator>() {};
        return MapBinder.newMapBinder(binder(), keyType, valueType);
    }
}
