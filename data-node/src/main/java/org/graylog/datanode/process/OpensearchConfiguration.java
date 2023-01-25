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
package org.graylog.datanode.process;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record OpensearchConfiguration(
        String opensearchVersion,
        Path opensearchDir,
        int httpPort,
        int transportPort,
        String clusterName, String nodeName, List<String> nodeRoles, List<String> networkHost,
        List<String> discoverySeedHosts, Map<String, String> additionalConfiguration
) {
    public Map<String, String> asMap() {

        Map<String, String> config = new LinkedHashMap<>();
        config.put("http.port", String.valueOf(httpPort));
        config.put("transport.port", String.valueOf(transportPort));
        if (clusterName != null && !clusterName.isBlank()) {
            config.put("cluster.name", clusterName);
        }
        if (nodeName != null && !nodeName.isBlank()) {
            config.put("node.name", nodeName);
        }
        if (nodeRoles != null && !nodeRoles.isEmpty()) {
            config.put("node.roles", toYamlList(nodeRoles));
        }
        if (networkHost != null && !networkHost.isEmpty()) {
            config.put("network.host", toYamlList(networkHost));
        }
        if (discoverySeedHosts != null && !discoverySeedHosts.isEmpty()) {
            config.put("discovery.seed_hosts", toYamlList(discoverySeedHosts));
        }
        config.putAll(additionalConfiguration);
        return config;
    }

    private String toYamlList(List<String> values) {
        return values.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.joining(","),
                        list -> "" + list + ""));
    }
}
