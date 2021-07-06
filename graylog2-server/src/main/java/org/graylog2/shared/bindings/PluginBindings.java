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

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.web.PluginUISettingsProvider;

import java.util.Set;

public class PluginBindings extends AbstractModule {
    private final Set<Plugin> plugins;

    public PluginBindings(Set<Plugin> plugins) {
        this.plugins = plugins;
    }

    @Override
    protected void configure() {
        final Multibinder<Plugin> pluginbinder = Multibinder.newSetBinder(binder(), Plugin.class);
        final Multibinder<PluginMetaData> pluginMetaDataBinder = Multibinder.newSetBinder(binder(), PluginMetaData.class);

        // Make sure there is a binding for the plugin rest resource classes to avoid binding errors when running
        // without plugins.
        MapBinder.newMapBinder(binder(), new TypeLiteral<String>() {},
                new TypeLiteral<Class<? extends PluginRestResource>>() {})
                .permitDuplicates();

        // Make sure there is a binding for the PluginUISettingsProvider classes to avoid binding errors when running
        // without plugins
        MapBinder.newMapBinder(binder(), String.class, PluginUISettingsProvider.class)
                .permitDuplicates();

        for (final Plugin plugin : plugins) {
            pluginbinder.addBinding().toInstance(plugin);
            for (final PluginModule pluginModule : plugin.modules()) {
                binder().install(pluginModule);
            }

            pluginMetaDataBinder.addBinding().toInstance(plugin.metadata());
        }
    }
}
