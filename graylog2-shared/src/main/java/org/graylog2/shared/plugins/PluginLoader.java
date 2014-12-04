/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.plugins;

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.Set;

public class PluginLoader {
    private static final Logger LOG = LoggerFactory.getLogger(PluginLoader.class);

    private final File pluginDir;

    public PluginLoader(File pluginDir) {
        this.pluginDir = pluginDir;
    }

    public Set<Plugin> loadPlugins() {
        final ImmutableSet.Builder<Plugin> plugins = ImmutableSet.builder();

        plugins.addAll(loadClassPathPlugins());
        plugins.addAll(loadJarPlugins());

        return plugins.build();
    }

    private Iterable<Plugin> loadClassPathPlugins() {
        return ServiceLoader.load(Plugin.class);
    }

    private Iterable<Plugin> loadJarPlugins() {
        if (!pluginDir.exists()) {
            LOG.warn("Plugin directory {} does not exist, not loading plugins.", pluginDir.getAbsolutePath());
            return Collections.emptySet();
        }

        if (!pluginDir.isDirectory()) {
            LOG.warn("Path {} is not a directory, cannot load plugins.", pluginDir);
            return Collections.emptySet();
        }

        LOG.debug("Scanning directory <{}> for plugins...", pluginDir.getAbsolutePath());
        final File[] files = pluginDir.listFiles();
        if (files == null) {
            LOG.warn("Could not list files in {}, cannot load plugins.", pluginDir);
            return Collections.emptySet();
        }

        LOG.debug("Loading [{}] plugins", files.length);
        final ClassLoader classLoader = getClass().getClassLoader();
        final ImmutableSet.Builder<Plugin> plugins = ImmutableSet.builder();
        for (File jar : files) {
            if (!jar.isFile()) {
                LOG.debug("{} is not a file, skipping.", jar);
            } else {
                try {
                    LOG.debug("Loading <" + jar.getAbsolutePath() + ">");
                    final ClassLoader pluginClassLoader = new URLClassLoader(new URL[]{jar.toURI().toURL()}, classLoader);
                    final ServiceLoader<Plugin> pluginServiceLoader = ServiceLoader.load(Plugin.class, pluginClassLoader);

                    plugins.addAll(pluginServiceLoader);
                } catch (MalformedURLException e) {
                    LOG.error("Cannot open JAR file for discovering plugins", e);
                }
            }
        }

        return plugins.build();
    }
}
