/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
