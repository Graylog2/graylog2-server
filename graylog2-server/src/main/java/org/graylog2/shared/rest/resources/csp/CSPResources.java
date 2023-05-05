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

import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
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
     * Return csp-list as a single string, based on all the properties with the specified group name
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
     * Merge all directives for the specified group into an existing csp-list.
     * We do not attempt to remove any duplicate values
     *
     * @param csp   Existing csp-list
     * @param group Group name of resource values to be merged
     * @return merged csp-list
     */
    public String merge(String csp, String group) {
        String result = "";
        String[] policyDirectives = csp.split(";");
        for (String policyDirective : policyDirectives) {
            int firstWhiteSpace = policyDirective.indexOf(" ");
            String directive = policyDirective.substring(0, firstWhiteSpace);
            String resource = cspResources.get(group, directive);
            result += policyDirective;
            if (!Strings.isNullOrEmpty(resource)) {
                result += " " + resource + ";";
            }
        }

        for (String policyDirective : cspResources.row(group).keySet()) {
            if (!result.contains(policyDirective)) {
                result += policyDirective + " " + cspResources.get(group, policyDirective) + ";";
            }
        }

        return result;
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
