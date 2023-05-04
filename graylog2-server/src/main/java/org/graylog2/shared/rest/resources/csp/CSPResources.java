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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.stream.Collectors;

public class CSPResources {
    private static final Logger LOG = LoggerFactory.getLogger(CSPResources.class);
    private static final String DEFAULT_FILE = "/org/graylog2/security/csp.config";
    private Table<String, String, String> cspResources;

    public CSPResources() {
        this(DEFAULT_FILE);
    }

    public CSPResources(String fileName) {
        try {
            cspResources = loadProperties(fileName);
        } catch (Exception e) {
            LOG.warn("Could not load config file {}: {}", fileName, e.getMessage());
            cspResources = HashBasedTable.create();
        }
    }

    /**
     * Return a CSP string based on all the properties with the specified group name
     *
     * @param group
     * @return CSP string
     */
    public String cspString(String group) {
        return cspResources.row(group).keySet().stream()
                .sorted()
                .map(key -> key + " " + cspResources.get(group, key))
                .collect(Collectors.joining(";"));
    }

    /**
     * Parse a property file where all property names are like 'group.key'.
     *
     * @param path path of resource file
     * @return table of property values
     * @throws IOException
     */
    Table<String, String, String> loadProperties(String path) throws IOException {
        InputStream inputStream = CSPResources.class.getResourceAsStream(path);
        Properties properties = new Properties();
        properties.load(inputStream);

        Table<String, String, String> resources = HashBasedTable.create();
        for (String propertyName : properties.stringPropertyNames()) {
            String[] substrings = propertyName.split("[.]");
            if (substrings.length != 2) {
                LOG.warn("Skipping malformed property {}: expecting format <group>.<key>", propertyName);
            } else {
                resources.put(substrings[0], substrings[1], properties.getProperty(propertyName));
            }
        }
        return resources;
    }

}
