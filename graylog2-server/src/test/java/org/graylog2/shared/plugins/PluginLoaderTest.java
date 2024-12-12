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
package org.graylog2.shared.plugins;

import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;
import org.graylog2.plugin.PreflightCheckModule;
import org.graylog2.plugin.SpecificNodePlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PluginLoaderTest {
    
    @Mock
    private ServiceLoader<Plugin> serviceLoader;

    @TempDir
    Path tempDir;
    ChainingClassLoader classLoader = new ChainingClassLoader(this.getClass().getClassLoader());

    @Test
    @SuppressWarnings("unchecked")
    public void testPluginFilter() {
        var plugin1 = Mockito.mock(ServiceLoader.Provider.class);
        when(plugin1.get()).thenReturn(new GraylogTestPlugin());
        var plugin2 = mock(ServiceLoader.Provider.class);
        when(plugin2.get()).thenReturn(new GraylogTestPlugin2());
        var plugin3 = mock(ServiceLoader.Provider.class);
        when(plugin3.get()).thenReturn(new DatanodeTestPlugin());

        when(serviceLoader.stream()).thenReturn(Stream.of(plugin1, plugin2, plugin3));
        PluginLoader pluginLoader = new PluginLoader(tempDir.toFile(), classLoader, null);
        Iterable<Plugin> plugins = pluginLoader.filterPlugins(serviceLoader);
        assertThat(plugins).hasSize(2);

        when(serviceLoader.stream()).thenReturn(Stream.of(plugin1, plugin2, plugin3));
        pluginLoader = new PluginLoader(tempDir.toFile(), classLoader, PluginLoader.NodeType.DATA_NODE);
        plugins = pluginLoader.filterPlugins(serviceLoader);
        assertThat(plugins).hasSize(1);
    }

    static class GraylogTestPlugin implements TestPlugin {}

    static class GraylogTestPlugin2 implements TestPlugin {}

    @SpecificNodePlugin(PluginLoader.NodeType.DATA_NODE)
    static class DatanodeTestPlugin implements TestPlugin {}

    interface TestPlugin extends Plugin {
        @Override
        default PluginMetaData metadata() {
            return null;
        }

        @Override
        default Collection<PluginModule> modules() {
            return List.of();
        }

        @Override
        default Collection<PreflightCheckModule> preflightCheckModules() {
            return Plugin.super.preflightCheckModules();
        }
    }

}
