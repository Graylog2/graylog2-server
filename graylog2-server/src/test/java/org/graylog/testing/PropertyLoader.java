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
package org.graylog.testing;

import com.google.common.io.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Properties;

/**
 * Loads properties from a resource.
 */
public class PropertyLoader {
    /**
     * Load the properties from the given resource. Returns an {@link UncheckedIOException} when loading the resources
     * failed.
     *
     * @param resourcePath the resource path
     * @return loaded properties
     */
    public static Properties loadProperties(String resourcePath) {
        final Properties properties = new Properties();
        final URL resource = Resources.getResource(resourcePath);
        try (InputStream stream = resource.openStream()) {
            properties.load(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Error while reading test properties " + resourcePath, e);
        }
        return properties;
    }

    public static String get(String resourcePath, String propertyName) {
        return loadProperties(resourcePath).getProperty(propertyName);
    }
}
