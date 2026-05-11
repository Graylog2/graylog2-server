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

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

/**
 * These are typed keys to the opense
 */
public class OpensearchDistributionProperties {
    private final Properties properties;

    public static OpensearchDistributionProperties forVersion(@Nonnull String version) {
        try (
                final InputStream stream = OpensearchDistributionProperties.class.getResourceAsStream(Path.of("/", "opensearch", "config", version, "distribution.properties").toString())
        ) {
            Properties properties = new Properties();
            properties.load(stream);
            return new OpensearchDistributionProperties(properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            throw new IllegalArgumentException(String.format("Opensearch distribution property '%s' not found", name));
        }
    }
}
