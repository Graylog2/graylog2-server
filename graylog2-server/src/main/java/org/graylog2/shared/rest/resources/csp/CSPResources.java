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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A Content Security Policy header consists of a list of policy directives, each of
 * which consists of a directive and one or more values:
 * <pre>
 * {@code
 * <CSP> ::= Content-Security-Policy: <csp-list>
 * <csp-list> :: = <policy-directive>{;<policy-directive>}
 * <policy-directive> ::= <directive> <value>{ <value>}
 * <directive> ::= default-src | script-src | ...
 * <value> ::= Strings that do not contain any white space
 * }
 * </pre>
 *
 * See https://content-security-policy.com/
 **/
public class CSPResources {
    private static final Logger LOG = LoggerFactory.getLogger(CSPResources.class);
    private static final String DEFAULT_FILE = "/org/graylog2/security/csp.config";
    private Table<String, String, Set<String>> cspTable;

    public CSPResources() {
        this(DEFAULT_FILE);
    }

    public CSPResources(String resourceName) {
        try {
            cspTable = loadProperties(resourceName);
        } catch (Exception e) {
            LOG.warn("Could not load config resource {}: {}", resourceName, e.getMessage());
            cspTable = HashBasedTable.create();
        }
    }

    /**
     * Return csp-list as a single string, based on all the properties with the specified group name
     *
     * @param group
     * @return CSP string
     */
    public String cspString(String group) {
        return cspTable.row(group).keySet().stream()
                .sorted()
                .map(key -> key + " "
                        + cspTable.get(group, key).stream().sorted().collect(Collectors.joining(" ")))
                .collect(Collectors.joining(";"));
    }

    /**
     * Update all existing groups (rows) in the table with the specified value. Duplicated values
     * are ignored; but substrings are tolerated e.g. test.com test.com:9999
     * Thread-safety needs to be enforced by the caller (i.e. CSPService).
     *
     * @param directive
     * @param value     a directive value, consisting of one or more entries separated by blanks
     */
    public void updateAll(String directive, String value) {
        Set<String> valueSet = new HashSet<>(Arrays.asList(value.split(" ")));

        cspTable.rowKeySet().forEach(group -> {
                    if (cspTable.get(group, directive) == null) {
                        cspTable.put(group, directive, valueSet);
                    } else {
                        cspTable.get(group, directive).addAll(valueSet);
                    }
                }
        );
    }

    /**
     * Parse a property file where all property names are like 'group.key'.
     *
     * @param path path of resource file
     * @return table of property values
     * @throws IOException
     */
    private Table<String, String, Set<String>> loadProperties(String path) throws IOException {
        InputStream inputStream = CSPResources.class.getResourceAsStream(path);
        Properties properties = new Properties();
        properties.load(inputStream);

        Table<String, String, Set<String>> resources = HashBasedTable.create();
        for (String propertyName : properties.stringPropertyNames()) {
            String[] substrings = propertyName.split("[.]");
            if (substrings.length != 2) {
                LOG.warn("Skipping malformed property {}: expecting format <group>.<key>", propertyName);
            } else {
                String[] valueArray = properties.getProperty(propertyName).split(" ");
                resources.put(substrings[0], substrings[1], new HashSet<>(Arrays.asList(valueArray)));
            }
        }
        return resources;
    }
}
