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
package org.graylog2.shared.rest.resources.csp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CSPResources {
    private static final Logger LOG = LoggerFactory.getLogger(CSPResources.class);
    private static final String DEFAULT_FILE = "/org/graylog2/security/csp.config";
    private Map<String, String> cspResources;

    public CSPResources() {
        this(DEFAULT_FILE);
    }

    public CSPResources(String fileName) {
        try {
            cspResources = loadProperties(fileName);
        } catch (IOException e) {
            LOG.warn("Could not load config file {}: {}", fileName, e.getMessage());
            cspResources = new HashMap<>();
        }
    }

    public String cspString() {
        return cspResources.keySet().stream()
                .sorted()
                .map(key -> key + " " + cspResources.get(key))
                .collect(Collectors.joining(";"));
    }

    Map<String, String> loadProperties(String fileName) throws IOException {
        InputStream inputStream = CSPResources.class.getResourceAsStream(fileName);
        Properties properties = new Properties();
        properties.load(inputStream);
        return properties.stringPropertyNames().stream()
                .collect(Collectors.toMap(Function.identity(), properties::getProperty));
    }

}
