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
package org.graylog.datanode;

import org.graylog.datanode.configuration.OpensearchConfigurationException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

public class OpensearchDistributionProperties {
    private final Properties properties;

    public static OpensearchDistributionProperties forVersion(@Nonnull String version) {
        final String resourcePath = "/opensearch/config/" + version + "/distribution.properties";
        try (final InputStream stream = OpensearchDistributionProperties.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("No distribution properties found for OpenSearch version " + version + " (looked for resource: " + resourcePath + ")");
            }
            Properties properties = new Properties();
            properties.load(stream);
            return new OpensearchDistributionProperties(properties);
        } catch (IOException e) {
            throw new OpensearchConfigurationException("Failed to load distribution properties for OpenSearch version " + version + " from resource: " + resourcePath, e);
        }
    }

    private OpensearchDistributionProperties(Properties properties) {
        this.properties = properties;
    }

    public String searchableSnapshotsRole() {
        return getProperty("searchable_snapshots_role");
    }

    private <T> T getProperty(String name) {
        if (properties.containsKey(name)) {
            return (T) properties.getProperty(name);
        } else {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Opensearch distribution property '%s' not found", name));
        }
    }
}
