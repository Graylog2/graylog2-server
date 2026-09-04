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
package org.graylog.datanode.bootstrap.plugin;

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginBootstrapConfig;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.graylog2.shared.plugins.PluginLoader;

import java.io.File;
import java.util.ServiceLoader;
import java.util.Set;

public class DatanodePluginLoader extends PluginLoader {

    public DatanodePluginLoader(File pluginDir, ChainingClassLoader classLoader) {
        super(pluginDir, classLoader);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<PluginBootstrapConfig> loadPluginBootstrapConfigs() {
        return ImmutableSet.copyOf(
                (ServiceLoader<PluginBootstrapConfig>) (ServiceLoader<?>)
                        ServiceLoader.load(DatanodePluginBootstrapConfig.class, classLoader)
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Iterable<Plugin> loadClassPathPlugins() {
        return (ServiceLoader<Plugin>) (ServiceLoader<?>)
                ServiceLoader.load(DatanodePlugin.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Iterable<Plugin> loadJarPlugins() {
        return ImmutableSet.copyOf(
                (ServiceLoader<Plugin>) (ServiceLoader<?>)
                        ServiceLoader.load(DatanodePlugin.class, classLoader)
        );
    }

}
