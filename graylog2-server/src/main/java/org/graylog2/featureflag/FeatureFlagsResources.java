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
package org.graylog2.featureflag;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;


class FeatureFlagsResources {

    Map<String, String> defaultProperties(String file) throws IOException {
        return loadProperties(FeatureFlagsResources.class.getResourceAsStream(file));
    }

    Map<String, String> customProperties(String file) throws IOException {
        return loadProperties(new FileInputStream(file));
    }

    private Map<String, String> loadProperties(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        return toMap(properties);
    }

    Map<String, String> systemProperties() {
        return toMap(System.getProperties());
    }

    Map<String, String> environmentVariables() {
        return System.getenv();
    }

    private static Map<String, String> toMap(Properties properties) {
        return properties.stringPropertyNames().stream()
                .collect(Collectors.toMap(Function.identity(), properties::getProperty));
    }
}
