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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;
import java.util.jar.JarFile;

import static java.util.Objects.requireNonNull;

public class PluginProperties {
    private static final Logger LOG = LoggerFactory.getLogger(PluginProperties.class);

    private static final String GRAYLOG_PLUGIN_PROPERTIES = "graylog-plugin.properties";

    private static final String PROPERTY_ISOLATED = "isolated";
    private static final String PROPERTY_ISOLATED_DEFAULT = "true";

    private final boolean isolated;

    public PluginProperties(Properties properties) {
        this.isolated = Boolean.parseBoolean((String) properties.getOrDefault(PROPERTY_ISOLATED, PROPERTY_ISOLATED_DEFAULT));
    }

    public static PluginProperties fromJarFile(final String filename) {
        // Try to load the plugin properties from the JAR file. This fails for plugins which do not have the
        // properties file.
        final Properties properties = new Properties();
        try {
            LOG.debug("Loading {} from {}", GRAYLOG_PLUGIN_PROPERTIES, filename);
            final JarFile jarFile = new JarFile(requireNonNull(filename));
            final InputStream inputStream = jarFile.getInputStream(jarFile.getEntry(GRAYLOG_PLUGIN_PROPERTIES));
            properties.load(inputStream);
        } catch (Exception e) {
            LOG.debug("Unable to load {} from plugin {}", GRAYLOG_PLUGIN_PROPERTIES, filename, e);
        }

        return new PluginProperties(properties);
    }

    public boolean isIsolated() {
        return this.isolated;
    }
}
