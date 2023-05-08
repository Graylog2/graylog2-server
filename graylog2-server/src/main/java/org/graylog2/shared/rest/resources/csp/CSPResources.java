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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
                .map(key -> key + " " + String.join(" ", cspTable.get(group, key)))
                .collect(Collectors.joining(";"));
    }

    /**
     * Update all existing groups (rows) in the table with the specified value.
     *
     * @param directive
     * @param value     a directive value, consisting of one or more entries separated by blanks
     */
    public void updateGroups(String directive, String value) {
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
     * Merge all directives for the specified group into an existing csp-list.
     * We do not attempt to remove any duplicate values
     *
     * @param csp   Existing csp-list
     * @param group Group name of resource values to be merged
     * @return merged csp-list
     */
    public String mergeWithResources(String csp, String group) {
        String result = "";
        String[] policyDirectives = csp.split(";");
        for (String s : policyDirectives) {
            String policyDirective = s.stripLeading();
            int firstWhiteSpace = policyDirective.indexOf(" ");
            String directive = policyDirective.substring(0, firstWhiteSpace);
            String resource = cspTable.get(group, directive);
            if (!Strings.isNullOrEmpty(resource)) {
                result += policyDirective + " " + resource + ";";
            } else {
                result += policyDirective + ";";
            }
        }

        for (String policyDirective : cspTable.row(group).keySet()) {
            if (!result.contains(policyDirective)) {
                result += policyDirective + " " + cspTable.get(group, policyDirective) + ";";
            }
        }

        return result;
    }

    /**
     * Merge 2 CSP policy directives: if a directive is present in both CSPs we concatenate the
     * values.
     *
     * @param csp1
     * @param csp2
     * @return Merged CSP
     */
    public String mergeCSPs(String csp1, String csp2) {
        Map<String, String> cspMap1 = cspToMap(csp1);
        Map<String, String> cspMap2 = cspToMap(csp2);
        for (Map.Entry<String, String> entry : cspMap2.entrySet()) {
            cspMap1.merge(entry.getKey(), entry.getValue(), (v1, v2) -> v1 + " " + v2);
        }
        return cspMap1.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(";"));
    }

    private Map<String, String> cspToMap(String csp) {
        Map<String, String> cspMap = new HashMap<>();
        Arrays.stream(csp.split(";"))
                .forEach(s -> {
                    String policyDirective = s.stripLeading();
                    int firstWhiteSpace = policyDirective.indexOf(" ");
                    String directive = policyDirective.substring(0, firstWhiteSpace);
                    cspMap.put(directive, policyDirective.substring(firstWhiteSpace + 1));
                });
        return cspMap;
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

    private void append(String directive, String value) {
        cspTable.rowKeySet().forEach(r -> {
            String existingValue = cspTable.get(r, directive);
            if (existingValue != null) {
                if (existingValue.contains(value))
                    cspTable.put(r, directive, existingValue + " " + value);
            } else {
                cspTable.put(r, directive, value);
            }
        });
    }

}
