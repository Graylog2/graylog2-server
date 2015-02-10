/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.plugins;

import com.google.common.base.Function;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

import static com.google.inject.internal.util.$Preconditions.checkNotNull;

public class PluginLoader {
    private static final Logger LOG = LoggerFactory.getLogger(PluginLoader.class);

    private final File pluginDir;

    public PluginLoader(File pluginDir) {
        this.pluginDir = pluginDir;
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

    private static class PluginAdapterFunction implements Function<Plugin, Plugin> {
        @Override
        public Plugin apply(Plugin input) {
            return new PluginAdapter(input);
        }
    }
}
