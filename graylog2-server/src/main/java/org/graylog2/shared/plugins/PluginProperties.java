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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

public class PluginProperties {
    private static final Logger LOG = LoggerFactory.getLogger(PluginProperties.class);

    private static final String MANIFEST_ATTRIBUTE = "Graylog-Plugin-Properties-Path";
    private static final String GRAYLOG_PLUGIN_PROPERTIES = "graylog-plugin.properties";

    private static final String PROPERTY_ISOLATED = "isolated";
    private static final String PROPERTY_ISOLATED_DEFAULT = "true";

    private final boolean isolated;

    public PluginProperties(Properties properties) {
        this.isolated = Boolean.parseBoolean((String) properties.getOrDefault(PROPERTY_ISOLATED, PROPERTY_ISOLATED_DEFAULT));
    }

    /**
     * Loads the Graylog plugin properties file from the given JAR file.
     *
     * The path to the properties file resource inside the JAR file is stored in the "Graylog-Plugin-Properties-Path"
     * attribute of the JAR manifest. (Example: {@code org.graylog.plugins.graylog-plugin-map-widget})
     *
     * If the plugin properties file does not exist or cannot be found (like in older plugins) a default
     * {@link PluginProperties} object will be returned.
     *
     * @param filename path to the JAR file
     * @return the plugin properties object
     */
    public static PluginProperties fromJarFile(final String filename) {
        final Properties properties = new Properties();
        try {
            final JarFile jarFile = new JarFile(requireNonNull(filename));
            final Optional<String> propertiesPath = getPropertiesPath(jarFile);

            if (propertiesPath.isPresent()) {
                LOG.debug("Loading <{}> from <{}>", propertiesPath.get(), filename);
                final ZipEntry entry = jarFile.getEntry(propertiesPath.get());

                if (entry != null) {
                    properties.load(jarFile.getInputStream(entry));
                } else {
                    LOG.debug("Plugin properties <{}> are missing in <{}>", propertiesPath.get(), filename);
                }
            }
        } catch (Exception e) {
            LOG.debug("Unable to load properties from plugin <{}>", filename, e);
        }

        return new PluginProperties(properties);
    }

    private static Optional<String> getPropertiesPath(final JarFile jarFile) throws IOException {
        // Lookup the resource path in the JAR manifest.
        final String value = jarFile.getManifest().getMainAttributes().getValue(MANIFEST_ATTRIBUTE);

        if (isNullOrEmpty(value)) {
            LOG.debug("No value found for attribute <{}> in JAR manifest of file <{}>", MANIFEST_ATTRIBUTE, jarFile.getName());
            return Optional.empty();
        } else {
            return Optional.of(value + "/" + GRAYLOG_PLUGIN_PROPERTIES);
        }
    }

    public boolean isIsolated() {
        return this.isolated;
    }
}
