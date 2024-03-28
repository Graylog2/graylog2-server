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

import org.apache.commons.exec.OS;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.configuration.S3RepositoryConfiguration;
import org.graylog.datanode.configuration.variants.OpensearchSecurityConfiguration;
import org.graylog.datanode.management.Environment;
import org.graylog.shaded.opensearch2.org.apache.http.HttpHost;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record OpensearchConfiguration(
        OpensearchDistribution opensearchDistribution,
        DatanodeDirectories datanodeDirectories,
        String bindAddress,
        String hostname,
        int httpPort,
        int transportPort,
        String clusterName,
        String nodeName,
        List<String> nodeRoles,
        List<String> discoverySeedHosts,
        OpensearchSecurityConfiguration opensearchSecurityConfiguration,
        S3RepositoryConfiguration s3RepositoryConfiguration,

        String nodeSearchCacheSize,
        Map<String, Object> additionalConfiguration
) {
    public Map<String, Object> asMap() {

        Map<String, Object> config = new LinkedHashMap<>();

        config.put("action.auto_create_index", "false");

        // currently, startup fails on macOS without disabling this filter.
        // for a description of the filter (although it's for ES), see https://www.elastic.co/guide/en/elasticsearch/reference/7.17/_system_call_filter_check.html
        if (OS.isFamilyMac()) {
            config.put("bootstrap.system_call_filter", "false");
        }

        if (bindAddress != null && !bindAddress.isBlank()) {
            config.put("network.host", bindAddress);
        }
        config.put("http.port", String.valueOf(httpPort));
        config.put("transport.port", String.valueOf(transportPort));
        if (clusterName != null && !clusterName.isBlank()) {
            config.put("cluster.name", clusterName);
        }

        config.put("node.name", nodeName);

        if (nodeRoles != null && !nodeRoles.isEmpty()) {
            config.put("node.roles", toValuesList(nodeRoles));
        }
        if (discoverySeedHosts != null && !discoverySeedHosts.isEmpty()) {
            config.put("discovery.seed_hosts", toValuesList(discoverySeedHosts));
        }

        config.put("discovery.seed_providers", "file");

        config.put("node.search.cache.size", nodeSearchCacheSize);
        if (s3RepositoryConfiguration.isRepositoryEnabled()) {
            config.putAll(s3RepositoryConfiguration.toOpensearchProperties());
        }

        config.putAll(additionalConfiguration);
        return config;
    }

    private String toValuesList(List<String> values) {
        return String.join(",", values);
    }

    public Environment getEnv() {
        final Environment env = new Environment(System.getenv());
        env.put("OPENSEARCH_PATH_CONF", datanodeDirectories.getOpensearchProcessConfigurationDir().toString());
        return env;
    }

    public HttpHost getRestBaseUrl() {
        final boolean sslEnabled = Boolean.parseBoolean(asMap().getOrDefault("plugins.security.ssl.http.enabled", "false").toString());
        return new HttpHost(hostname(), httpPort(), sslEnabled ? "https" : "http");
    }

    public HttpHost getClusterBaseUrl() {
        final boolean sslEnabled = Boolean.parseBoolean(asMap().getOrDefault("plugins.security.ssl.http.enabled", "false").toString());
        return new HttpHost(hostname(), transportPort(), sslEnabled ? "https" : "http");
    }

    public boolean securityConfigured() {
        return opensearchSecurityConfiguration() != null;
    }
}
