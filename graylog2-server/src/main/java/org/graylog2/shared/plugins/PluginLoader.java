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

import com.google.common.base.Function;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.inject.Injector;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;
import org.graylog2.shared.SuppressForbidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

public class PluginLoader {
    private static final Logger LOG = LoggerFactory.getLogger(PluginLoader.class);

    private final File pluginDir;
    private final ChainingClassLoader classLoader;
    private final Injector coreConfigInjector;

    public PluginLoader(File pluginDir, ChainingClassLoader classLoader, Injector coreConfigInjector) {
        this.pluginDir = requireNonNull(pluginDir);
        this.classLoader = requireNonNull(classLoader);
        this.coreConfigInjector = coreConfigInjector;
    }

    public Set<Plugin> loadPlugins() {
        return ImmutableSortedSet.orderedBy(new PluginComparator())
                .addAll(Iterables.transform(loadJarPlugins(), new PluginAdapterFunction()))
                .addAll(Iterables.transform(loadClassPathPlugins(), new PluginAdapterFunction()))
                .build();
    }

    private Iterable<Plugin> loadClassPathPlugins() {
        return ServiceLoader.load(Plugin.class);
    }

    @SuppressForbidden("Deliberate invocation of URL#getFile()")
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
        final List<URL> urls = Arrays.stream(files)
                .filter(File::isFile)
                .map(jar -> {
                    try {
                        LOG.debug("Loading <" + jar.getAbsolutePath() + ">");
                        return jar.toURI().toURL();
                    } catch (MalformedURLException e) {
                        LOG.error("Cannot open JAR file for discovering plugins", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        final List<URL> sharedClassLoaderUrls = new ArrayList<>();
        urls.forEach(url -> {
            final PluginProperties properties = PluginProperties.fromJarFile(url.getFile());

            // Decide whether to create a separate, isolated class loader for the plugin. When the plugin is isolated
            // (the default), it gets its own class loader and cannot see other plugins. Plugins which are not
            // isolated share one class loader so they can see each other. (makes plugin inter-dependencies work)
            if (properties.isIsolated()) {
                LOG.debug("Creating isolated class loader for <{}>", url);
                classLoader.addClassLoader(URLClassLoader.newInstance(new URL[]{url}));
            } else {
                LOG.debug("Using shared class loader for <{}>", url);
                sharedClassLoaderUrls.add(url);
            }
        });

        // Only create the shared class loader if any plugin requests to be shared.
        if (!sharedClassLoaderUrls.isEmpty()) {
            LOG.debug("Creating shared class loader for {} plugins: {}", sharedClassLoaderUrls.size(), sharedClassLoaderUrls);
            classLoader.addClassLoader(URLClassLoader.newInstance(sharedClassLoaderUrls.toArray(new URL[sharedClassLoaderUrls.size()])));
        }

        final ServiceLoader<Plugin> pluginServiceLoader = ServiceLoader.load(Plugin.class, classLoader);

        return ImmutableSet.copyOf(pluginServiceLoader);
    }

    public static class PluginComparator implements Comparator<Plugin> {
        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(Plugin o1, Plugin o2) {
            return ComparisonChain.start()
                    .compare(o1.metadata().getUniqueId(), o2.metadata().getUniqueId())
                    .compare(o1.metadata().getName(), o2.metadata().getName())
                    .compare(o1.metadata().getVersion(), o2.metadata().getVersion())
                    .result();
        }
    }

    /**
     * Adapter for {@link org.graylog2.plugin.Plugin} which implements {@link #equals(Object)} and {@link #hashCode()}
     * only taking {@link org.graylog2.plugin.PluginMetaData#getUniqueId()} into account.
     */
    public static class PluginAdapter implements Plugin {
        private final Plugin plugin;

        public PluginAdapter(Plugin plugin) {
            this.plugin = checkNotNull(plugin);
        }

        @Override
        public PluginMetaData metadata() {
            return plugin.metadata();
        }

        @Override
        public Collection<PluginModule> modules() {
            return plugin.modules();
        }

        public String getPluginClassName() {
            return plugin.getClass().getCanonicalName();
        }

        @Override
        public int hashCode() {
            return Objects.hash(plugin.metadata().getUniqueId());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Plugin) {
                final Plugin that = (Plugin) obj;
                return Objects.equals(this.metadata().getUniqueId(), that.metadata().getUniqueId());
            }

            return false;
        }

        @Override
        public String toString() {
            final PluginMetaData metadata = plugin.metadata();
            return metadata.getName() + " " + metadata.getVersion() + " [" + metadata.getUniqueId() + "]";
        }
    }

    private class PluginAdapterFunction implements Function<Plugin, Plugin> {
        @Override
        public Plugin apply(Plugin input) {
            coreConfigInjector.injectMembers(input);
            return new PluginAdapter(input);
        }
    }
}
