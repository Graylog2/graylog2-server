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

import com.google.common.collect.Sets;
import org.graylog2.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;

public class PluginLoader {
    private static final Logger LOG = LoggerFactory.getLogger(PluginLoader.class);

    public static final String GRAYLOG2_PLUGIN_PROPERTIES = "graylog2-plugin.properties";

    private final File pluginDir;

    public PluginLoader(File pluginDir) {
        this.pluginDir = pluginDir;
    }

    public Set<Plugin> loadPlugins() {
        final HashSet<Plugin> plugins = Sets.newHashSet();

        plugins.addAll(loadClassPathPlugins());
        plugins.addAll(loadJarPlugins());

        return plugins;
    }

    private Set<Plugin> loadClassPathPlugins() {
        final HashSet<Plugin> plugins = Sets.newHashSet();
        final ClassLoader cl = getClass().getClassLoader();
        final Set<Class<? extends Plugin>> classes = loadPluginClasses(cl);
        for (Class<? extends Plugin> pluginClass : classes) {
            final Plugin plugin = instantiatePlugin(pluginClass);
            if (plugin != null) {
                plugins.add(plugin);
            }
        }

        return plugins;
    }

    public Set<Plugin> loadJarPlugins() {
        final HashSet<Plugin> plugins = Sets.newHashSet();

        if (!pluginDir.exists()) {
            LOG.warn("Plugin directory {} does not exist, not loading plugins.", pluginDir.getAbsolutePath());
            return plugins;
        }
        if (!pluginDir.isDirectory()) {
            LOG.warn("Path {} is not a directory, cannot load plugins.", pluginDir);
            return plugins;
        }

        final ClassLoader classLoader = getClass().getClassLoader();

        LOG.debug("Scanning directory <{}> for plugins...", pluginDir.getAbsolutePath());
        final File[] files = pluginDir.listFiles();
        LOG.debug("Loading [{}] plugins", files.length);
        for (File jar : files) {
            try {
                // just try to read the plugin.properties file, so we can be sure the URLClassLoader doesn't fall back to the system classloader
                final JarFile jarFile = new JarFile(jar);
                jarFile.getEntry(GRAYLOG2_PLUGIN_PROPERTIES);

                LOG.debug("Loading <" + jar.getAbsolutePath() + ">");
                final URLClassLoader pluginClassLoader = new URLClassLoader(new URL[]{ jar.toURI().toURL() }, classLoader);

                final Set<Class<? extends Plugin>> pluginClasses = loadPluginClasses(pluginClassLoader);
                for (Class<? extends Plugin> pluginClass : pluginClasses) {
                    LOG.debug("Found plugin " + pluginClass);
                    final Plugin plugin = instantiatePlugin(pluginClass);
                    if (plugin != null) {
                        plugins.add(plugin);
                    }
                }
            } catch (MalformedURLException e) {
                LOG.error("Cannot open jar for discovering plugins", e);
            } catch (IOException e) {
                LOG.debug("Skipping file {}, not a plugin jar", jar.getAbsolutePath());
            }
        }

        return plugins;
    }

    @SuppressWarnings("unchecked")
    private Set<Class<? extends Plugin>> loadPluginClasses(ClassLoader cl) {
        Set<Class<? extends Plugin>> pluginClasses = Sets.newHashSet();
        final Enumeration<URL> urls;
        try {
            urls = cl.getResources(GRAYLOG2_PLUGIN_PROPERTIES);
        } catch (IOException e) {
            LOG.error("Unable to read resources from class loader", e);
            return pluginClasses;
        }

        while (urls.hasMoreElements()) {
            final URL url = urls.nextElement();
            final Properties properties = new Properties();
            try {
                final InputStream inputStream = url.openStream();
                properties.load(inputStream);
            } catch (IOException e) {
                LOG.error("Unable to read plugin properties file", e);
            }
            final String pluginClassName = properties.getProperty("plugin");
            LOG.debug("Plugin class name is {}", pluginClassName);
            if (pluginClassName == null) {
                LOG.error("Missing plugin property in property descriptor file: {}", url);
                continue;
            }
            Class<? extends Plugin> pluginClass;
            try {
                pluginClass = (Class<? extends Plugin>) cl.loadClass(pluginClassName);
                pluginClasses.add(pluginClass);
            } catch (ClassNotFoundException e) {
                LOG.error("Unable to find plugin class {}, skipping.", pluginClasses);
            }
        }
        return pluginClasses;
    }

    public Plugin instantiatePlugin(Class<? extends Plugin> pluginClass) {
        final Plugin plugin;
        try {
            plugin = pluginClass.getConstructor().newInstance();
        } catch (Exception e) {
            LOG.error("Cannot find constructor for plugin " + pluginClass.getCanonicalName() + ". It must have a public no-args constructor.", e);
            return null;
        }
        return plugin;
    }
}
